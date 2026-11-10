package ai.linfra.resource_app.conversation.model

import ai.linfra.resource_app.ranker.model.RankedPick

/** A validated proposal waiting for the user to confirm. Stored on the assistant message that presented it. */
data class Draft(
    val requirements: VmRequirements,
    val pick: RankedPick,
    val explanation: String,
)
