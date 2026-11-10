package ai.linfra.resource_app.terraform

import ai.linfra.resource_app.terraform.TerraformFixtures.arm
import ai.linfra.resource_app.terraform.TerraformFixtures.burstable
import ai.linfra.resource_app.terraform.TerraformFixtures.compiler
import ai.linfra.resource_app.terraform.TerraformFixtures.dedicatedLocalDisk
import ai.linfra.resource_app.terraform.TerraformFixtures.intent
import ai.linfra.resource_app.terraform.TerraformFixtures.tfvars
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DockerVmModuleTest {

    @Test
    fun `uses the docker-vm module`() {
        assertEquals("docker-vm", compiler.compile(intent(burstable)).module)
    }

    @Test
    fun `burstable type gets CPU shares and a network disk`() {
        val intent = intent(burstable)

        val vars = tfvars(intent)

        assertEquals(intent.id.toString(), vars["intent_id"].asText())
        assertEquals(intent.tenantId.toString(), vars["tenant_id"].asText())
        assertEquals("burst.medium", vars["instance_type"].asText())
        assertEquals("local-eu", vars["region"].asText())
        assertEquals(2, vars["vcpu"].asInt())
        assertEquals(4096, vars["memory_mb"].asInt())
        assertFalse(vars["dedicated_cpu"].asBoolean())
        assertEquals("amd64", vars["architecture"].asText())
        assertEquals(20, vars["network_disk_gb"].asInt())
    }

    @Test
    fun `dedicated type with its own disk gets pinned cores and no network disk`() {
        val vars = tfvars(intent(dedicatedLocalDisk))

        assertEquals("storage.medium", vars["instance_type"].asText())
        assertTrue(vars["dedicated_cpu"].asBoolean())
        assertEquals(0, vars["network_disk_gb"].asInt())
    }

    @Test
    fun `ARM type runs an arm64 image`() {
        assertEquals("arm64", tfvars(intent(arm))["architecture"].asText())
    }
}
