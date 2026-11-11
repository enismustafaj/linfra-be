package ai.linfra.resource_app.remediation

import ai.linfra.resource_app.remediation.model.FindingStatus
import ai.linfra.resource_app.remediation.model.FindingType
import ai.linfra.resource_app.remediation.model.RemediationAction
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** How reconciliation reports become findings over time. */
class FindingSyncTest : RemediationTest() {

    @Test
    fun `a problem seen on consecutive runs stays one open finding`() {
        val intent = plannedIntent()

        reconcileWith(observed("c-1", intent))
        val first = open(FindingType.ORPHAN)
        reconcileWith(observed("c-1", intent))

        assertEquals(listOf(first.id), findings.findAll().map { it.id })
        assertTrue(reload(first).lastSeenAt >= first.lastSeenAt)
    }

    @Test
    fun `a container labeled with an unknown tenant becomes an unattributed finding, not a failed sync`() {
        val stranger = observed("c-1", null).copy(intentId = java.util.UUID.randomUUID(), tenantId = java.util.UUID.randomUUID())

        reconcileWith(stranger)

        assertEquals(null, open(FindingType.ORPHAN).tenantId)
    }

    @Test
    fun `a problem that disappears is cleared`() {
        reconcileWith(observed("c-1", plannedIntent()))
        val finding = open(FindingType.ORPHAN)

        reconcileWith()

        assertEquals(FindingStatus.CLEARED, reload(finding).status)
    }

    @Test
    fun `a dismissed subject is not reported again, another subject is`() {
        val intent = plannedIntent()
        reconcileWith(observed("c-1", intent))
        remediation.resolve(intent.tenantId, open(FindingType.ORPHAN).id, RemediationAction.DISMISS, null)

        reconcileWith(observed("c-1", intent), observed("c-2", intent))

        val openOrphans = findings.findAllByStatus(FindingStatus.OPEN).filter { it.type == FindingType.ORPHAN }
        assertEquals(listOf(listOf("c-2")), openOrphans.map { it.cloudIds })
    }

    @Test
    fun `accepting a drift hides that drift but not a different one later`() {
        val resource = recorded(plannedIntent())
        reconcileWith(observed(resource.cloudId, intents.findById(resource.intentId).get(), memoryGb = 8))
        remediation.resolve(resource.tenantId, open(FindingType.DRIFT).id, RemediationAction.ACCEPT, null)

        reconcileWith(observed(resource.cloudId, intents.findById(resource.intentId).get(), memoryGb = 8))
        assertTrue(findings.findAllByStatus(FindingStatus.OPEN).isEmpty())

        reconcileWith(observed(resource.cloudId, intents.findById(resource.intentId).get(), memoryGb = 16))
        assertEquals("16", (open(FindingType.DRIFT).details["differences"] as Map<*, *>).let { (it["memory_gb"] as Map<*, *>)["actual"] })
    }
}
