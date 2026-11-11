package ai.linfra.resource_app.conversation.rest.dto

import ai.linfra.resource_app.conversation.persistence.Conversation
import java.time.Instant
import java.util.UUID

data class ConversationSummary(val id: UUID, val title: String?, val createdAt: Instant) {
    companion object {
        fun of(c: Conversation) = ConversationSummary(c.id, c.title, c.createdAt)
    }
}
