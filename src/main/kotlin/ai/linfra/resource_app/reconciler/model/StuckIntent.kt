package ai.linfra.resource_app.reconciler.model

import ai.linfra.resource_app.intent.model.IntentState
import java.util.UUID

/** Should be in progress, but no worker has held a live lease on it for longer than the grace period. */
data class StuckIntent(val intentId: UUID, val tenantId: UUID, val state: IntentState, val stuckForSeconds: Long)
