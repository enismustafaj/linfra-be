package ai.linfra.resource_app.terraform.service

import ai.linfra.resource_app.terraform.model.CompiledWorkspace
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails
import org.springframework.stereotype.Service
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

class TofuException(message: String) : RuntimeException(message)

@Service
class TofuRunner(
    db: JdbcConnectionDetails,
    @Value("\${linfra.tofu.work-dir}") private val workDir: File,
    @Value("\${linfra.tofu.plugin-cache-dir}") private val pluginCacheDir: File,
    @Value("\${linfra.tofu.timeout}") private val timeout: Duration,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    // Same database as Linfra (from Spring's connection details, so Testcontainers works too); state in its own schema.
    private val connString = PgConnString.fromJdbc(db.jdbcUrl, db.username, db.password)
    private val initLock = Any()

    /** [runId] must be unique per attempt (intent + fencing token) so a stale worker never shares a directory. */
    fun apply(workspace: CompiledWorkspace, stateName: String, runId: String): Map<String, Any?> =
        inWorkspace(workspace, stateName, runId) { dir ->
            tofu(dir, "apply", "-auto-approve")
            outputs(dir)
        }

    fun destroy(workspace: CompiledWorkspace, stateName: String, runId: String) {
        inWorkspace(workspace, stateName, runId) { dir -> tofu(dir, "destroy", "-auto-approve") }
    }

    private fun <T> inWorkspace(workspace: CompiledWorkspace, stateName: String, runId: String, block: (File) -> T): T {
        val dir = File(workDir, runId).apply { deleteRecursively(); mkdirs() }
        try {
            workspace.moduleFiles.forEach { (name, content) -> File(dir, name).writeText(content) }
            File(dir, TFVARS_FILE).writeText(workspace.tfvarsJson)
            // ponytail: init serialised per process because the pg backend's first-run schema creation races;
            // across processes it only matters on the very first run against an empty database
            synchronized(initLock) { tofu(dir, "init") }
            tofu(dir, "workspace", "select", "-or-create=true", stateName)
            return block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun outputs(dir: File): Map<String, Any?> =
        (objectMapper.readValue(tofu(dir, "output", "-json"), Map::class.java) as Map<String, Map<String, Any?>>)
            .mapValues { it.value["value"] }

    private fun tofu(dir: File, vararg args: String): String {
        pluginCacheDir.mkdirs()
        // "workspace" subcommands don't take -no-color
        val command = listOf("tofu", *args) + if (args[0] == "workspace") emptyList() else listOf("-no-color")
        val process = ProcessBuilder(command)
            .directory(dir)
            .redirectErrorStream(true)
            .apply {
                environment()["PG_CONN_STR"] = connString
                environment()["TF_PLUGIN_CACHE_DIR"] = pluginCacheDir.path
                environment()["TF_IN_AUTOMATION"] = "1"
                environment()["TF_INPUT"] = "0"
            }
            .start()
        val output = StringBuilder()
        val reader = Thread { process.inputStream.bufferedReader().forEachLine { output.appendLine(it) } }.apply { start() }
        if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            process.descendants().forEach { it.destroyForcibly() }
            process.destroyForcibly()
            throw TofuException("tofu ${args.first()} timed out after $timeout")
        }
        reader.join()
        if (process.exitValue() != 0) {
            log.warn("tofu {} failed:\n{}", args.joinToString(" "), output)
            throw TofuException("tofu ${args.joinToString(" ")} exited ${process.exitValue()}: ${output.takeLast(500)}")
        }
        return output.toString()
    }
}
