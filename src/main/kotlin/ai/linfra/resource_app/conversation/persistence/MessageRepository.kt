package ai.linfra.resource_app.conversation.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MessageRepository : JpaRepository<Message, Long> {
    fun findAllByConversationIdOrderById(conversationId: UUID): List<Message>
    fun findFirstByConversationIdAndDraftIsNotNullOrderByIdDesc(conversationId: UUID): Message?
}
