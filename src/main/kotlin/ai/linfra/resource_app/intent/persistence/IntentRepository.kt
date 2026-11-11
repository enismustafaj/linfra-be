package ai.linfra.resource_app.intent.persistence

import ai.linfra.resource_app.intent.model.IntentState
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface IntentRepository : JpaRepository<Intent, UUID> {
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): Intent?
    fun findAllByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<Intent>
    fun findAllByTenantIdAndStateIn(tenantId: UUID, states: Collection<IntentState>): List<Intent>
    fun findBySourceMessageId(sourceMessageId: Long): Intent?
    fun findAllByConversationIdOrderByCreatedAt(conversationId: UUID): List<Intent>
}
