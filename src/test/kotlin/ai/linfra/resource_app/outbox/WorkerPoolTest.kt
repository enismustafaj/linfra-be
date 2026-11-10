package ai.linfra.resource_app.outbox

import ai.linfra.resource_app.IntegrationTest
import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.outbox.model.Claim
import ai.linfra.resource_app.outbox.service.OutboxWorker
import ai.linfra.resource_app.outbox.service.WorkerPool
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkerPoolTest : IntegrationTest() {

    @Autowired lateinit var worker: OutboxWorker

    @Test
    fun `several threads drain the outbox and apply each intent exactly once`() {
        val intents = (1..6).map { approvedIntent() }
        whenever(provisioner.find(any())) doReturn null
        whenever(provisioner.apply(any())).thenAnswer { provisioned("srv-${(it.arguments[0] as Claim).intentId}") }
        val pool = WorkerPool(worker, "test", threadCount = 3, pollInterval = Duration.ofMillis(20), lease = lease)

        pool.start()
        try {
            val deadline = System.currentTimeMillis() + 10_000
            while (intents.any { state(it.id) != IntentState.SUCCEEDED } && System.currentTimeMillis() < deadline) Thread.sleep(50)
        } finally {
            pool.stop()
        }

        intents.forEach { assertEquals(IntentState.SUCCEEDED, state(it.id)) }
        verify(provisioner, times(6)).apply(any())
        val workerIds = intents.flatMap { runs.findAllByIntentIdOrderById(it.id) }.map { it.workerId }.toSet()
        assertTrue(workerIds.all { it.startsWith("test-") }, "$workerIds")
    }
}
