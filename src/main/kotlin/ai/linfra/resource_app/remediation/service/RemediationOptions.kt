package ai.linfra.resource_app.remediation.service

import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.intent.persistence.IntentRepository
import ai.linfra.resource_app.remediation.model.FindingStatus
import ai.linfra.resource_app.remediation.model.FindingType
import ai.linfra.resource_app.remediation.model.RemediationAction
import ai.linfra.resource_app.remediation.model.RemediationAction.ACCEPT
import ai.linfra.resource_app.remediation.model.RemediationAction.ADOPT
import ai.linfra.resource_app.remediation.model.RemediationAction.DESTROY
import ai.linfra.resource_app.remediation.model.RemediationAction.DISMISS
import ai.linfra.resource_app.remediation.model.RemediationAction.KEEP
import ai.linfra.resource_app.remediation.model.RemediationAction.MARK_DESTROYED
import ai.linfra.resource_app.remediation.model.RemediationAction.MARK_NEEDS_ATTENTION
import ai.linfra.resource_app.remediation.model.RemediationAction.RETRY_NOW
import ai.linfra.resource_app.remediation.persistence.Finding
import ai.linfra.resource_app.resource.persistence.ResourceRepository
import org.springframework.stereotype.Service

/** The actions a finding offers right now. Computed on every read and again before acting, so they never go stale. */
@Service
class RemediationOptions(
    private val intents: IntentRepository,
    private val resources: ResourceRepository,
) {
    fun actionsFor(f: Finding): List<RemediationAction> {
        if (f.status != FindingStatus.OPEN) return emptyList()
        return when (f.type) {
            FindingType.ORPHAN -> listOfNotNull(DESTROY, ADOPT.takeIf { canAdopt(f) }, DISMISS)
            FindingType.DUPLICATE -> listOf(KEEP)
            FindingType.MISSING -> listOf(MARK_DESTROYED, DISMISS)
            FindingType.DRIFT -> listOf(ACCEPT)
            FindingType.STUCK -> listOfNotNull(RETRY_NOW, MARK_NEEDS_ATTENTION.takeIf { intentState(f) == IntentState.APPLYING })
        }
    }

    /** Only an intent already handed to a human, of the same tenant, with no resource recorded yet. */
    private fun canAdopt(f: Finding): Boolean {
        val intent = f.intentId?.let { intents.findById(it).orElse(null) } ?: return false
        return intent.tenantId == f.tenantId && intent.state == IntentState.NEEDS_ATTENTION && resources.findByIntentId(intent.id) == null
    }

    private fun intentState(f: Finding) = f.intentId?.let { intents.findById(it).orElse(null) }?.state
}
