package ai.linfra.resource_app.conversation.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ConversationRepository : JpaRepository<Conversation, UUID> {
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): Conversation?
}
