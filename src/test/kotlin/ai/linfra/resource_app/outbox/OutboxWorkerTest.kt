package ai.linfra.resource_app.outbox

import ai.linfra.resource_app.IntegrationTest
import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.outbox.model.Claim
import ai.linfra.resource_app.outbox.model.JobStatus
import ai.linfra.resource_app.outbox.service.OutboxWorker
import ai.linfra.resource_app.resource.model.ResourceStatus
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class OutboxWorkerTest : IntegrationTest() {

    @Autowired lateinit var worker: OutboxWorker

    @Test
    fun `nothing to claim returns false`() {
        assertFalse(worker.processNext("w"))
    }

    @Test
    fun `applies once and records the resource with the intent`() {
        val intent = approvedIntent()
        whenever(provisioner.find(any())) doReturn null
        whenever(provisioner.apply(any())) doReturn provisioned("srv-1")

        worker.processNext("w")

        assertEquals(IntentState.SUCCEEDED, state(intent.id))
        val resource = resources.findByIntentId(intent.id)!!
        assertEquals("srv-1", resource.cloudId)
        assertEquals(intent.tenantId, resource.tenantId)
        assertEquals(ResourceStatus.ACTIVE, resource.status)
        assertEquals("172.17.0.9", resource.outputs["ipv4"])
        assertEquals(listOf("SUCCEEDED"), runs.findAllByIntentIdOrderById(intent.id).map { it.outcome })
    }

    @Test
    fun `takes over a dead worker's job and adopts instead of re-creating`() {
        val intent = approvedIntent()
        claimFor(intent.id, Duration.ZERO) // previous worker died mid-apply; its create reached the cloud
        whenever(provisioner.find(any())) doReturn provisioned("srv-from-dead-worker")

        worker.processNext("w")

        verify(provisioner, never()).apply(any())
        verify(provisioner).adopt(any<Claim>(), any()) // brought into OpenTofu's state too
        assertEquals("srv-from-dead-worker", resources.findByIntentId(intent.id)!!.cloudId)
    }

    @Test
    fun `repeated failure ends in NEEDS_ATTENTION, not stuck`() {
        val intent = approvedIntent()
        whenever(provisioner.find(any())) doReturn null
        whenever(provisioner.apply(any())) doThrow RuntimeException("cloud 500")

        repeat(2) { makeDueNow(intent.id); worker.processNext("w") } // max-attempts=2 in tests

        assertEquals(IntentState.NEEDS_ATTENTION, state(intent.id))
        assertEquals(JobStatus.DEAD, jobs.findByIntentId(intent.id)!!.status)
        assertEquals(listOf("RETRY", "NEEDS_ATTENTION"), runs.findAllByIntentIdOrderById(intent.id).map { it.outcome })
    }

    @Test
    fun `a cloud resource already owned by another intent is never adopted twice`() {
        val first = approvedIntent()
        whenever(provisioner.find(any())) doReturn provisioned("srv-shared")
        worker.processNext("w")
        assertEquals(IntentState.SUCCEEDED, state(first.id))

        // A buggy find() returns the same server for a different intent: the unique (provider, cloud_id) refuses it.
        val second = approvedIntent()
        repeat(2) { makeDueNow(second.id); worker.processNext("w") }

        assertEquals(IntentState.NEEDS_ATTENTION, state(second.id))
        assertEquals(null, resources.findByIntentId(second.id))
    }
}
