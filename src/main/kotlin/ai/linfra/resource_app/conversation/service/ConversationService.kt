package ai.linfra.resource_app.conversation.service

import ai.linfra.resource_app.conversation.exception.ConversationNotFoundException
import ai.linfra.resource_app.conversation.persistence.Conversation
import ai.linfra.resource_app.conversation.persistence.ConversationRepository
import ai.linfra.resource_app.conversation.persistence.Message
import ai.linfra.resource_app.conversation.persistence.MessageRepository
import ai.linfra.resource_app.intent.persistence.Intent
import ai.linfra.resource_app.intent.persistence.IntentRepository
import org.springframework.stereotype.Service
import java.util.UUID

private const val TITLE_CHARS = 60

/** Conversation lifecycle and reads. Every access goes through requireOwned, so tenant scoping lives in one place. */
@Service
class ConversationService(
    private val conversations: ConversationRepository,
    private val messages: MessageRepository,
    private val intents: IntentRepository,
) {
    fun start(tenantId: UUID): Conversation = conversations.save(Conversation(tenantId = tenantId))

    fun list(tenantId: UUID): List<Conversation> = conversations.findAllByTenantIdOrderByCreatedAtDesc(tenantId)

    /** Names the conversation after its first message. */
    fun titleIfMissing(conversation: Conversation, firstMessage: String) {
        if (conversation.title != null) return
        conversation.title = firstMessage.lineSequence().first().take(TITLE_CHARS)
        conversations.save(conversation)
    }

    fun requireOwned(tenantId: UUID, conversationId: UUID): Conversation =
        conversations.findByIdAndTenantId(conversationId, tenantId) ?: throw ConversationNotFoundException(conversationId)

    fun history(conversationId: UUID): List<Message> = messages.findAllByConversationIdOrderById(conversationId)

    fun linkedIntents(conversationId: UUID): List<Intent> = intents.findAllByConversationIdOrderByCreatedAt(conversationId)
}
