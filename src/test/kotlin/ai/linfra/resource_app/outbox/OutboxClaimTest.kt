package ai.linfra.resource_app.outbox

import ai.linfra.resource_app.IntegrationTest
import ai.linfra.resource_app.intent.model.IntentState
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OutboxClaimTest : IntegrationTest() {

    @Autowired lateinit var tx: TransactionTemplate

    @Test
    fun `claim moves the intent to APPLYING and records the run before acting`() {
        val intent = approvedIntent()

        val claim = claimFor(intent.id)

        assertEquals(IntentState.APPLYING, state(intent.id))
        assertEquals(1, claim.fencingToken)
        assertEquals(listOf(null), runs.findAllByIntentIdOrderById(intent.id).map { it.finishedAt })
    }

    @Test
    fun `two workers never claim the same job`() {
        val intent = approvedIntent()
        val holding = CountDownLatch(1)
        val release = CountDownLatch(1)
        // Worker A claims inside an open transaction, so its row lock is held until we release it.
        val a = CompletableFuture.supplyAsync {
            tx.execute { claimFor(intent.id).also { holding.countDown(); release.await(5, TimeUnit.SECONDS) } }
        }
        holding.await(5, TimeUnit.SECONDS)

        val b = store.claim("w-b", lease) // SKIP LOCKED: does not block, does not get A's row
        release.countDown()

        assertEquals(intent.id, a.get().intentId)
        assertNull(b)
    }

    @Test
    fun `expired approval never starts applying`() {
        val intent = approvedIntent(ttl = Duration.ofMillis(-1))

        assertNull(store.claim("w", lease))

        assertEquals(IntentState.EXPIRED, state(intent.id))
    }
}
