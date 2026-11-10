package ai.linfra.resource_app.reconciler.model

import java.time.Instant
import java.util.UUID

data class ReconciliationReport(
    val checkedAt: Instant,
    /** Resource ids confirmed to exist; used to update last_seen_at, not exposed per tenant. */
    val seen: List<UUID>,
    val missing: List<MissingResource>,
    val drifted: List<Drift>,
    val orphans: List<Orphan>,
    val duplicates: List<Duplicate>,
    val stuck: List<StuckIntent>,
    val oldestUnfinishedIntentAgeSeconds: Long?,
) {
    /** A tenant only sees findings about its own resources and intents. Unattributable orphans are operator-only (metrics). */
    fun forTenant(tenantId: UUID) = copy(
        seen = emptyList(),
        missing = missing.filter { it.tenantId == tenantId },
        drifted = drifted.filter { it.tenantId == tenantId },
        orphans = orphans.filter { it.tenantId == tenantId },
        duplicates = duplicates.filter { it.tenantId == tenantId },
        stuck = stuck.filter { it.tenantId == tenantId },
        oldestUnfinishedIntentAgeSeconds = null,
    )
}
