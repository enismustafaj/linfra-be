package ai.linfra.resource_app.intent

import ai.linfra.resource_app.IntegrationTest
import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.outbox.model.JobStatus
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class IntentApprovalTest : IntegrationTest() {

    @Test
    fun `approval for a different plan hash changes nothing`() {
        val intent = plannedIntent()

        assertFailsWith<IllegalStateException> { intentService.approve(intent.tenantId, intent.id, "other-hash", Duration.ofMinutes(5)) }

        assertEquals(IntentState.PLANNED, state(intent.id))
        assertNull(jobs.findByIntentId(intent.id))
    }

    @Test
    fun `approval and outbox job commit together`() {
        val intent = plannedIntent()

        intentService.approve(intent.tenantId, intent.id, "h1", Duration.ofMinutes(5))

        assertEquals(IntentState.APPROVED, state(intent.id))
        assertEquals(JobStatus.PENDING, jobs.findByIntentId(intent.id)!!.status)
    }
}
