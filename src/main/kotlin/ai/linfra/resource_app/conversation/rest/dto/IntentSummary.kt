package ai.linfra.resource_app.conversation.rest.dto

import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.intent.persistence.Intent
import java.util.UUID

data class IntentSummary(val id: UUID, val state: IntentState, val sourceMessageId: Long?) {
    companion object {
        fun of(i: Intent) = IntentSummary(i.id, i.state, i.sourceMessageId)
    }
}
