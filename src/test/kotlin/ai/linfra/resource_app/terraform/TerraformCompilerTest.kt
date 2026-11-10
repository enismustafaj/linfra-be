package ai.linfra.resource_app.terraform

import ai.linfra.resource_app.terraform.TerraformFixtures.compiler
import ai.linfra.resource_app.terraform.TerraformFixtures.burstable
import ai.linfra.resource_app.terraform.TerraformFixtures.intent
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TerraformCompilerTest {

    @Test
    fun `the same intent always compiles to the same workspace and hash`() {
        val intent = intent(burstable)

        assertEquals(compiler.compile(intent), compiler.compile(intent))
    }

    @Test
    fun `different intents get different hashes`() {
        assertNotEquals(compiler.compile(intent(burstable)).sha256, compiler.compile(intent(burstable)).sha256)
    }

    @Test
    fun `workspace ships the module's files and lock file, but no test files`() {
        assertEquals(
            setOf(".terraform.lock.hcl", "main.tf", "outputs.tf", "variables.tf", "versions.tf"),
            compiler.compile(intent(burstable)).moduleFiles.keys,
        )
    }
}
