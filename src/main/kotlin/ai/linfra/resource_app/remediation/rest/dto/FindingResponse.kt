package ai.linfra.resource_app.remediation.rest.dto

import ai.linfra.resource_app.ranker.model.Provider
import ai.linfra.resource_app.remediation.model.FindingStatus
import ai.linfra.resource_app.remediation.model.FindingType
import ai.linfra.resource_app.remediation.model.RemediationAction
import ai.linfra.resource_app.remediation.persistence.Finding
import java.time.Instant
import java.util.UUID

data class FindingResponse(
    val id: UUID,
    val type: FindingType,
    val status: FindingStatus,
    val intentId: UUID?,
    val resourceId: UUID?,
    val provider: Provider?,
    val cloudIds: List<String>,
    val details: Map<String, Any?>,
    val firstSeenAt: Instant,
    val lastSeenAt: Instant,
    val resolvedAt: Instant?,
    val resolution: RemediationAction?,
    val resolutionDetail: String?,
    /** What the caller can do about it now; empty once the finding is closed. */
    val availableActions: List<RemediationAction>,
) {
    companion object {
        fun of(f: Finding, actions: List<RemediationAction>) = FindingResponse(
            f.id, f.type, f.status, f.intentId, f.resourceId, f.provider, f.cloudIds, f.details,
            f.firstSeenAt, f.lastSeenAt, f.resolvedAt, f.resolution, f.resolutionDetail, actions,
        )
    }
}
