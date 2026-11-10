package ai.linfra.resource_app.terraform

import ai.linfra.resource_app.terraform.TerraformFixtures.compiler
import ai.linfra.resource_app.terraform.TerraformFixtures.burstable
import ai.linfra.resource_app.terraform.TerraformFixtures.intent
import ai.linfra.resource_app.terraform.TerraformFixtures.modules
import ai.linfra.resource_app.terraform.exception.NotInAllowListException
import ai.linfra.resource_app.terraform.exception.UnsafeTerraformValueException
import ai.linfra.resource_app.terraform.service.TerraformValues
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/** Linfra only generates tfvars values, and only allow-listed ones. The module's validation blocks are tested by tofu test. */
class TerraformInjectionTest {

    private val payload = "\${file(\"/etc/passwd\")}"

    @Test
    fun `a tampered instance type is rejected by the catalog allow-list`() {
        val tampered = intent(burstable) { it.withObject("/pick/entry").put("instanceType", payload) }

        assertFailsWith<NotInAllowListException> { compiler.compile(tampered) }
    }

    @Test
    fun `a tampered region is rejected`() {
        val tampered = intent(burstable) { it.withObject("/pick/entry").put("region", "local-eu\"}, \"x\": {\"") }

        assertFailsWith<NotInAllowListException> { compiler.compile(tampered) }
    }

    @Test
    fun `an instance type that exists elsewhere but not in the requested region is rejected`() {
        val tampered = intent(burstable) { it.withObject("/pick/entry").put("region", "local-xx") }

        assertFailsWith<NotInAllowListException> { compiler.compile(tampered) }
    }

    @Test
    fun `an out-of-range disk is rejected`() {
        val tampered = intent(burstable) { it.withObject("/pick").put("diskGb", 100_000) }

        assertFailsWith<NotInAllowListException> { compiler.compile(tampered) }
    }

    @Test
    fun `free text from the user never reaches the workspace`() {
        val tfvars = compiler.compile(intent(burstable.copy(purpose = "$payload %{ for x in y }"))).tfvarsJson

        assertFalse(tfvars.contains("/etc/passwd"))
        assertFalse(tfvars.contains("%{"))
        assertFalse(tfvars.contains("\${"))
    }

    @Test
    fun `tfvars set exactly the variables each module declares`() {
        listOf(burstable, TerraformFixtures.dedicatedLocalDisk, TerraformFixtures.arm).forEach { req ->
            val workspace = compiler.compile(intent(req))
            val keys = TerraformFixtures.mapper.readTree(workspace.tfvarsJson).fieldNames().asSequence().toSet()
            assertEquals(modules.declaredVariables(workspace.module), keys)
        }
    }

    @Test
    fun `the value backstop rejects template syntax, quotes and newlines`() {
        listOf(payload, "%{if true}", "a\"b", "a\nb", "", "x".repeat(64)).forEach {
            assertFailsWith<UnsafeTerraformValueException>(it) { TerraformValues.requireSafe(it) }
        }
        assertEquals("t3.small", TerraformValues.requireSafe("t3.small"))
        assertEquals("eu-central-1", TerraformValues.requireSafe("eu-central-1"))
    }
}
