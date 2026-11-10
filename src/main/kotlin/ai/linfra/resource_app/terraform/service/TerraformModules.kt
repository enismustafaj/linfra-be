package ai.linfra.resource_app.terraform.service

import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

const val LOCK_FILE = ".terraform.lock.hcl"

/** Reads the checked-in modules from the classpath: every .tf file in resources/terraform/<name>, plus its lock file. */
@Component
class TerraformModules {
    private val resolver = PathMatchingResourcePatternResolver()
    private val cache = ConcurrentHashMap<String, Map<String, String>>()
    private val variablePattern = Regex("""^variable\s+"([a-z_]+)"""", RegexOption.MULTILINE)

    /** File name → content, sorted by name so hashing is deterministic. */
    fun files(module: String): Map<String, String> = cache.getOrPut(module) {
        // The lock file pins provider checksums and lets tofu reuse the plugin cache instead of re-downloading.
        (resolver.getResources("classpath:terraform/$module/*.tf") + resolver.getResources("classpath:terraform/$module/$LOCK_FILE"))
            .filter { it.exists() }
            .associate { it.filename!! to it.inputStream.use { s -> s.readBytes().decodeToString() } }
            .toSortedMap()
            .also { check(it.isNotEmpty()) { "Terraform module '$module' not found on the classpath" } }
    }

    /** Names of the variables the module declares. */
    fun declaredVariables(module: String): Set<String> =
        files(module).values.flatMap { variablePattern.findAll(it).map { m -> m.groupValues[1] } }.toSet()
}
