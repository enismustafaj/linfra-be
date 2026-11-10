package ai.linfra.resource_app.reconciler.model

import ai.linfra.resource_app.intent.model.IntentState
import java.time.Instant
import java.util.UUID

/** An APPROVED or APPLYING intent with what the reconciler needs to judge whether anyone is working on it. */
data class InFlightIntent(
    val intentId: UUID,
    val tenantId: UUID,
    val state: IntentState,
    val approvedAt: Instant,
    /** When a worker should have been on it: the job's due time or when its last lease ran out. */
    val dueSince: Instant,
    /** Set while a worker holds an unexpired lease. */
    val liveLeaseUntil: Instant?,
)
