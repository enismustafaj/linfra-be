package ai.linfra.resource_app.remediation

import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.remediation.exception.ActionNotAllowedException
import ai.linfra.resource_app.remediation.model.FindingStatus
import ai.linfra.resource_app.remediation.model.FindingType
import ai.linfra.resource_app.remediation.model.RemediationAction.ADOPT
import ai.linfra.resource_app.remediation.model.RemediationAction.DESTROY
import ai.linfra.resource_app.remediation.model.RemediationAction.DISMISS
import ai.linfra.resource_app.remediation.service.RemediationOptions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OrphanRemediationTest : RemediationTest() {

    @Autowired lateinit var options: RemediationOptions

    @Test
    fun `DESTROY removes the container and resolves the finding`() {
        val intent = plannedIntent()
        reconcileWith(observed("c-1", intent))

        val resolved = remediation.resolve(intent.tenantId, open(FindingType.ORPHAN).id, DESTROY, null)

        verify(remover).remove("c-1")
        assertEquals(FindingStatus.RESOLVED, resolved.status)
        assertEquals(DESTROY, resolved.resolution)
    }

    @Test
    fun `ADOPT is only offered for an intent already handed to a human`() {
        val intent = plannedIntent()
        reconcileWith(observed("c-1", intent))

        assertEquals(listOf(DESTROY, DISMISS), options.actionsFor(open(FindingType.ORPHAN)))
        assertFailsWith<ActionNotAllowedException> { remediation.resolve(intent.tenantId, open(FindingType.ORPHAN).id, ADOPT, null) }
    }

    @Test
    fun `ADOPT records the container for a NEEDS_ATTENTION intent and completes it`() {
        val intent = draftIntent(IntentState.NEEDS_ATTENTION)
        reconcileWith(observed("c-1", intent))
        whenever(finder.find(any())) doReturn provisioned("c-1")

        remediation.resolve(intent.tenantId, open(FindingType.ORPHAN).id, ADOPT, null)

        assertEquals("c-1", resources.findByIntentId(intent.id)!!.cloudId)
        assertEquals(IntentState.SUCCEEDED, state(intent.id))
        verify(provisioner).adopt(eq(intent.id), any(), any(), any(), any())
    }

    @Test
    fun `an action can only be applied once`() {
        val intent = plannedIntent()
        reconcileWith(observed("c-1", intent))
        val finding = open(FindingType.ORPHAN)
        remediation.resolve(intent.tenantId, finding.id, DISMISS, null)

        assertFailsWith<ActionNotAllowedException> { remediation.resolve(intent.tenantId, finding.id, DESTROY, null) }
    }
}
