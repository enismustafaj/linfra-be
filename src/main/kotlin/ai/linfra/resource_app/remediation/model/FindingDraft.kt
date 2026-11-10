package ai.linfra.resource_app.remediation.model

import ai.linfra.resource_app.ranker.model.Provider
import java.util.UUID

/** One reconciliation finding in the shape the findings table stores, before it's matched to an existing row. */
data class FindingDraft(
    val type: FindingType,
    val subjectKey: String,
    val tenantId: UUID?,
    val intentId: UUID?,
    val resourceId: UUID?,
    val provider: Provider?,
    val cloudIds: List<String>,
    val details: Map<String, Any?>,
)
