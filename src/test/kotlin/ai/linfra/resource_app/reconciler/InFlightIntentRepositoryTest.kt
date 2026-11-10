package ai.linfra.resource_app.reconciler

import ai.linfra.resource_app.IntegrationTest
import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.reconciler.persistence.InFlightIntentRepository
import ai.linfra.resource_app.reconciler.service.Reconciler
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The one query that joins intents with their outbox job's lease. */
class InFlightIntentRepositoryTest : IntegrationTest() {

    @Autowired lateinit var inFlight: InFlightIntentRepository
    @Autowired lateinit var reconciler: Reconciler

    private val now: Instant = Instant.now()

    @Test
    fun `an approved intent waiting for a worker is in flight with no live lease`() {
        val intent = approvedIntent()

        val row = inFlight.findInFlight(now).single()

        assertEquals(intent.id, row.intentId)
        assertEquals(IntentState.APPROVED, row.state)
        assertNull(row.liveLeaseUntil)
    }

    @Test
    fun `a live lease is reported, an expired one is not but marks when the worker was last on it`() {
        val live = approvedIntent().also { claimFor(it.id) }
        val expired = approvedIntent()
        val leaseEnd = now.minusSeconds(600).truncatedTo(ChronoUnit.MICROS)
        claimFor(expired.id, Duration.ZERO).let { jobs.findByIntentId(expired.id)!!.apply { leaseUntil = leaseEnd; jobs.save(this) } }

        val rows = inFlight.findInFlight(now).associateBy { it.intentId }

        assertNotNull(rows.getValue(live.id).liveLeaseUntil)
        assertNull(rows.getValue(expired.id).liveLeaseUntil)
        assertTrue(rows.getValue(expired.id).dueSince >= leaseEnd)
    }

    @Test
    fun `finished intents are not in flight`() {
        val intent = approvedIntent()
        intents.findById(intent.id).get().apply { moveTo(IntentState.APPLYING); moveTo(IntentState.SUCCEEDED); intents.save(this) }

        assertTrue(inFlight.findInFlight(now).isEmpty())
    }

    @Test
    fun `the reconciler reports an intent nobody picked up within the grace period as stuck`() {
        val intent = approvedIntent()
        jobs.findByIntentId(intent.id)!!.apply { availableAt = now.minus(Duration.ofMinutes(10)); jobs.save(this) }
        intents.findById(intent.id).get().apply { approvedAt = now.minus(Duration.ofMinutes(10)); intents.save(this) }
        whenever(finder.listManaged()) doReturn emptyList()

        val stuck = reconciler.reconcile().stuck.single()

        assertEquals(intent.id, stuck.intentId)
        assertTrue(stuck.stuckForSeconds >= 590, "${stuck.stuckForSeconds}")
    }
}
