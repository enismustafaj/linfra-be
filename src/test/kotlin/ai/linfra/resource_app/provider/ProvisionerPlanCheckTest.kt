package ai.linfra.resource_app.provider

import ai.linfra.resource_app.outbox.model.Claim
import ai.linfra.resource_app.provider.exception.PlanChangedException
import ai.linfra.resource_app.provider.service.Provisioner
import ai.linfra.resource_app.terraform.TerraformFixtures
import ai.linfra.resource_app.terraform.service.TofuRunner
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** The worker applies only the exact workspace that was approved. */
class ProvisionerPlanCheckTest {

    private val tofu = mock<TofuRunner>()
    private val provisioner = Provisioner(TerraformFixtures.compiler, tofu, emptyList())
    private val intent = TerraformFixtures.intent(TerraformFixtures.burstable)
    private fun claim(planHash: String) = Claim(1, intent.id, intent.tenantId, intent.spec, 1, 1, planHash)

    @Test
    fun `refuses to apply when the compiled workspace no longer matches the approved plan`() {
        assertFailsWith<PlanChangedException> { provisioner.apply(claim("stale-hash")) }
        verify(tofu, never()).apply(any(), any(), any())
    }

    @Test
    fun `applies when it matches`() {
        whenever(tofu.apply(any(), any(), any())) doReturn mapOf("resource_id" to "c-1", "ipv4" to "172.17.0.2")

        val result = provisioner.apply(claim(TerraformFixtures.compiler.compile(intent).sha256))

        assertEquals("c-1", result.cloudId)
    }
}
