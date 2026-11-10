package ai.linfra.resource_app.remediation

import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.intent.persistence.Intent
import ai.linfra.resource_app.outbox.model.JobStatus
import ai.linfra.resource_app.remediation.exception.ActionNotAllowedException
import ai.linfra.resource_app.remediation.model.FindingType
import ai.linfra.resource_app.remediation.model.RemediationAction.MARK_NEEDS_ATTENTION
import ai.linfra.resource_app.remediation.model.RemediationAction.RETRY_NOW
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StuckRemediationTest : RemediationTest() {

    /** Approved 20 minutes ago, APPLYING, its worker's lease ran out 10 minutes ago, nobody took over. */
    private fun stuckIntent(): Intent {
        val intent = approvedIntent()
        claimFor(intent.id, Duration.ZERO)
        val longAgo = Instant.now().minus(Duration.ofMinutes(20))
        intents.findById(intent.id).get().apply { approvedAt = longAgo; intents.save(this) }
        jobs.findByIntentId(intent.id)!!.apply {
            availableAt = longAgo
            leaseUntil = Instant.now().minus(Duration.ofMinutes(10))
            jobs.save(this)
        }
        reconcileWith()
        return intent
    }

    @Test
    fun `RETRY_NOW makes the job claimable immediately`() {
        val intent = stuckIntent()

        remediation.resolve(intent.tenantId, open(FindingType.STUCK).id, RETRY_NOW, null)

        val job = jobs.findByIntentId(intent.id)!!
        assertEquals(JobStatus.PENDING, job.status)
        assertTrue(!job.availableAt.isAfter(Instant.now()))
    }

    @Test
    fun `MARK_NEEDS_ATTENTION stops retries and hands the intent to a human`() {
        val intent = stuckIntent()

        remediation.resolve(intent.tenantId, open(FindingType.STUCK).id, MARK_NEEDS_ATTENTION, null)

        assertEquals(IntentState.NEEDS_ATTENTION, state(intent.id))
        assertEquals(JobStatus.DEAD, jobs.findByIntentId(intent.id)!!.status)
    }

    @Test
    fun `refuses when a worker picked the intent up in the meantime`() {
        val intent = stuckIntent()
        val finding = open(FindingType.STUCK)
        makeDueNow(intent.id)
        claimFor(intent.id) // a worker holds a live lease again

        assertFailsWith<ActionNotAllowedException> { remediation.resolve(intent.tenantId, finding.id, RETRY_NOW, null) }
    }
}
