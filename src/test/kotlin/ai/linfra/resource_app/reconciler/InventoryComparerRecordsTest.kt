package ai.linfra.resource_app.reconciler

import ai.linfra.resource_app.reconciler.ReconcilerFixtures.compare
import ai.linfra.resource_app.reconciler.ReconcilerFixtures.observed
import ai.linfra.resource_app.reconciler.ReconcilerFixtures.recorded
import ai.linfra.resource_app.reconciler.model.Difference
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Recorded resources against what the provider has: seen, missing, drifted. */
class InventoryComparerRecordsTest {

    @Test
    fun `a recorded resource that exists unchanged is seen and nothing else`() {
        val resource = recorded()

        val report = compare(observed = listOf(observed(resource.cloudId, resource.intentId)), recorded = listOf(resource))

        assertEquals(listOf(resource.id), report.seen)
        assertTrue(report.missing.isEmpty() && report.drifted.isEmpty() && report.orphans.isEmpty())
    }

    @Test
    fun `a recorded resource the provider no longer has is missing`() {
        val resource = recorded()

        val report = compare(recorded = listOf(resource))

        assertEquals(listOf(resource.id), report.missing.map { it.resourceId })
        assertTrue(report.seen.isEmpty())
    }

    @Test
    fun `resized memory and unpinned cores are reported as drift with expected and actual`() {
        val resource = recorded()
        val changed = observed(resource.cloudId, resource.intentId).copy(memoryGb = 8, dedicatedCpu = true)

        val drift = compare(observed = listOf(changed), recorded = listOf(resource)).drifted.single()

        assertEquals(
            mapOf("memory_gb" to Difference("4", "8"), "dedicated_cpu" to Difference("false", "true")),
            drift.differences,
        )
    }

    @Test
    fun `a relabeled instance type is drift`() {
        val resource = recorded()
        val relabeled = observed(resource.cloudId, resource.intentId).copy(instanceType = "dedicated.large")

        val drift = compare(observed = listOf(relabeled), recorded = listOf(resource)).drifted.single()

        assertEquals(Difference("burst.medium", "dedicated.large"), drift.differences["instance_type"])
    }

    @Test
    fun `attributes the provider does not report are not drift`() {
        val resource = recorded()
        val partial = observed(resource.cloudId, resource.intentId).copy(vcpu = null, memoryGb = null)

        assertTrue(compare(observed = listOf(partial), recorded = listOf(resource)).drifted.isEmpty())
    }
}
