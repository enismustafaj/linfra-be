package ai.linfra.resource_app.reconciler

import ai.linfra.resource_app.reconciler.ReconcilerFixtures.compare
import ai.linfra.resource_app.reconciler.ReconcilerFixtures.inFlight
import ai.linfra.resource_app.reconciler.ReconcilerFixtures.observed
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Unrecorded resources and in-flight intents: orphans, duplicates, stuck intents. */
class InventoryComparerIntentsTest {

    @Test
    fun `an unrecorded resource whose intent is not in progress is an orphan`() {
        val report = compare(observed = listOf(observed("c-1")))

        assertEquals("c-1", report.orphans.single().cloudId)
    }

    @Test
    fun `an unrecorded resource whose intent is still in flight is not an orphan yet`() {
        val intentId = UUID.randomUUID()

        val report = compare(observed = listOf(observed("c-1", intentId)), inFlight = listOf(inFlight(intentId, Duration.ofSeconds(5), liveLease = true)))

        assertTrue(report.orphans.isEmpty())
    }

    @Test
    fun `a resource without an intent label is an orphan`() {
        assertEquals("no intent_id label", compare(observed = listOf(observed("c-1", intentId = null))).orphans.single().reason)
    }

    @Test
    fun `two resources with the same intent are a duplicate`() {
        val intentId = UUID.randomUUID()

        val duplicate = compare(observed = listOf(observed("c-2", intentId), observed("c-1", intentId))).duplicates.single()

        assertEquals(intentId, duplicate.intentId)
        assertEquals(listOf("c-1", "c-2"), duplicate.cloudIds)
    }

    @Test
    fun `an in-flight intent with no live lease past the grace period is stuck`() {
        val intentId = UUID.randomUUID()

        val stuck = compare(inFlight = listOf(inFlight(intentId, Duration.ofMinutes(5)))).stuck.single()

        assertEquals(intentId, stuck.intentId)
        assertEquals(300, stuck.stuckForSeconds)
    }

    @Test
    fun `a live lease or a short wait is not stuck`() {
        val report = compare(
            inFlight = listOf(
                inFlight(UUID.randomUUID(), Duration.ofMinutes(10), liveLease = true),
                inFlight(UUID.randomUUID(), Duration.ofSeconds(30)),
            ),
        )

        assertTrue(report.stuck.isEmpty())
    }

    @Test
    fun `oldest unfinished intent age comes from the earliest approval`() {
        assertNull(compare().oldestUnfinishedIntentAgeSeconds)
        assertEquals(360, compare(inFlight = listOf(inFlight(UUID.randomUUID(), Duration.ofMinutes(5)))).oldestUnfinishedIntentAgeSeconds)
    }
}
