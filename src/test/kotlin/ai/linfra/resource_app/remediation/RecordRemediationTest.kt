package ai.linfra.resource_app.remediation

import ai.linfra.resource_app.remediation.exception.ActionNotAllowedException
import ai.linfra.resource_app.remediation.model.FindingType
import ai.linfra.resource_app.remediation.model.RemediationAction.KEEP
import ai.linfra.resource_app.remediation.model.RemediationAction.MARK_DESTROYED
import ai.linfra.resource_app.resource.model.ResourceStatus
import org.junit.jupiter.api.Test
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Duplicates and missing resources. */
class RecordRemediationTest : RemediationTest() {

    @Test
    fun `KEEP removes the other copies and points the record at the one kept`() {
        val intent = plannedIntent()
        val resource = recorded(intent, cloudId = "c-1")
        reconcileWith(observed("c-1", intent), observed("c-2", intent))

        remediation.resolve(intent.tenantId, open(FindingType.DUPLICATE).id, KEEP, "c-2")

        verify(remover).remove("c-1")
        verify(remover, never()).remove("c-2")
        assertEquals("c-2", resources.findById(resource.id).get().cloudId)
    }

    @Test
    fun `KEEP needs one of the finding's own cloud ids`() {
        val intent = plannedIntent()
        reconcileWith(observed("c-1", intent), observed("c-2", intent))

        assertFailsWith<ActionNotAllowedException> { remediation.resolve(intent.tenantId, open(FindingType.DUPLICATE).id, KEEP, "c-9") }
        verify(remover, never()).remove("c-1")
    }

    @Test
    fun `MARK_DESTROYED accepts that a missing resource is gone`() {
        val resource = recorded(plannedIntent())
        reconcileWith()

        remediation.resolve(resource.tenantId, open(FindingType.MISSING).id, MARK_DESTROYED, null)

        assertEquals(ResourceStatus.DESTROYED, resources.findById(resource.id).get().status)
    }
}
