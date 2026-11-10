package ai.linfra.resource_app.reconciler.model

import java.time.Instant
import java.util.UUID

data class ReconciliationReport(
    val checkedAt: Instant,
    /** Resource ids confirmed to exist; used to update last_seen_at. */
    val seen: List<UUID>,
    val missing: List<MissingResource>,
    val drifted: List<Drift>,
    val orphans: List<Orphan>,
    val duplicates: List<Duplicate>,
    val stuck: List<StuckIntent>,
    val oldestUnfinishedIntentAgeSeconds: Long?,
)
