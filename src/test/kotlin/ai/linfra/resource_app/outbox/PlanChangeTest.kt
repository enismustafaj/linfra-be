package ai.linfra.resource_app.outbox

import ai.linfra.resource_app.IntegrationTest
import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.outbox.model.JobStatus
import ai.linfra.resource_app.outbox.service.OutboxWorker
import ai.linfra.resource_app.provider.exception.PlanChangedException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class PlanChangeTest : IntegrationTest() {

    @Autowired lateinit var worker: OutboxWorker

    @Test
    fun `a plan that changed since approval expires the intent instead of applying or retrying`() {
        val intent = approvedIntent()
        whenever(provisioner.find(any())) doReturn null
        whenever(provisioner.apply(any())) doThrow PlanChangedException(intent.id)

        worker.processNext("w")

        assertEquals(IntentState.EXPIRED, state(intent.id))
        assertEquals(JobStatus.DONE, jobs.findByIntentId(intent.id)!!.status)
        assertEquals(listOf("EXPIRED"), runs.findAllByIntentIdOrderById(intent.id).map { it.outcome })
    }
}
