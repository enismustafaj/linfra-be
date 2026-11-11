package ai.linfra.resource_app.provider

import ai.linfra.resource_app.provider.model.ProvisionedResource
import ai.linfra.resource_app.provider.service.Provisioner
import ai.linfra.resource_app.ranker.model.Provider
import ai.linfra.resource_app.terraform.TerraformFixtures
import ai.linfra.resource_app.terraform.service.TofuRunner
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Adopting a found resource imports exactly the module's resources into the intent's own state. */
class ProvisionerAdoptTest {

    private val tofu = mock<TofuRunner>()
    private val provisioner = Provisioner(TerraformFixtures.compiler, tofu, emptyList())
    private val found = ProvisionedResource(Provider.DOCKER, "local-eu", "burst.medium", "c-123", emptyMap())

    @Test
    fun `a type with a network disk imports the container and its volume`() {
        val intent = TerraformFixtures.intent(TerraformFixtures.burstable)
        whenever(tofu.importMissing(any(), any(), any(), any())) doReturn emptyList()

        provisioner.adopt(intent.id, intent.tenantId, intent.spec, found, "run-1")

        verify(tofu).importMissing(
            any(), eq("intent-${intent.id}"), eq("run-1"),
            eq(mapOf("docker_container.vm" to "c-123", "docker_volume.disk[0]" to "linfra-${intent.id}")),
        )
    }

    @Test
    fun `a type with its own local disk imports only the container`() {
        val intent = TerraformFixtures.intent(TerraformFixtures.dedicatedLocalDisk)
        whenever(tofu.importMissing(any(), any(), any(), any())) doReturn emptyList()

        provisioner.adopt(intent.id, intent.tenantId, intent.spec, found, "run-1")

        verify(tofu).importMissing(any(), any(), any(), eq(mapOf("docker_container.vm" to "c-123")))
    }
}
