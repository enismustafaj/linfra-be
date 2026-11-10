package ai.linfra.resource_app.terraform

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

/**
 * Runs each module's own tofu tests (tests/validation.tftest.hcl) with mock providers: fmt, validate, and the
 * variable validation blocks against injection payloads. Downloads providers (AWS is several hundred MB), so it is
 * excluded from the default build: run with `mvn verify -Ptofu`. Skipped when tofu is not installed.
 */
@Tag("tofu")
class TerraformModuleTest {

    private val tofuInstalled = runCatching { ProcessBuilder("tofu", "version").start().waitFor() == 0 }.getOrDefault(false)

    @ParameterizedTest
    @ValueSource(strings = ["docker-vm"])
    fun `module passes fmt, validate and its validation tests`(module: String) {
        assumeTrue(tofuInstalled, "tofu not installed")
        val dir = Files.createTempDirectory("linfra-$module").toFile()
        File("src/main/resources/terraform/$module").copyRecursively(dir)

        tofu(dir, "fmt", "-check", "-recursive")
        tofu(dir, "init", "-backend=false", "-input=false")
        tofu(dir, "validate")
        tofu(dir, "test")
        dir.deleteRecursively()
    }

    private fun tofu(dir: File, vararg args: String) {
        val cache = File(System.getProperty("user.home"), ".cache/linfra-tofu-plugins").apply { mkdirs() }
        val process = ProcessBuilder("tofu", *args).directory(dir).redirectErrorStream(true)
            .apply { environment()["TF_PLUGIN_CACHE_DIR"] = cache.path; environment()["TF_IN_AUTOMATION"] = "1" }
            .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor(10, TimeUnit.MINUTES)
        assertEquals(0, process.exitValue(), "tofu ${args.joinToString(" ")} failed:\n$output")
    }
}
