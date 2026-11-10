package ai.linfra.resource_app.conversation.rest.dto

import ai.linfra.resource_app.conversation.model.Draft
import ai.linfra.resource_app.conversation.model.MessageRole
import ai.linfra.resource_app.conversation.persistence.Message
import java.time.Instant

data class MessageResponse(
    val id: Long,
    val role: MessageRole,
    val content: String,
    val draft: Draft?,
    val createdAt: Instant,
) {
    companion object {
        fun of(m: Message) = MessageResponse(m.id!!, m.role, m.content, m.draft, m.createdAt)
    }
}
