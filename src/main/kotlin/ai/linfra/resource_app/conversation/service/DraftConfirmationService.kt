package ai.linfra.resource_app.conversation.service

import ai.linfra.resource_app.conversation.exception.DraftNotConfirmableException
import ai.linfra.resource_app.conversation.persistence.MessageRepository
import ai.linfra.resource_app.intent.persistence.Intent
import ai.linfra.resource_app.intent.persistence.IntentRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

/** Turns the latest confirmed draft into a PROPOSED intent. Idempotent: one draft yields at most one intent. */
@Service
class DraftConfirmationService(
    private val conversations: ConversationService,
    private val messages: MessageRepository,
    private val intents: IntentRepository,
    private val objectMapper: ObjectMapper,
    private val tx: TransactionTemplate,
) {
    fun confirm(tenantId: UUID, conversationId: UUID, messageId: Long): Intent =
        try {
            tx.execute { createOnce(tenantId, conversationId, messageId) }!!
        } catch (e: DataIntegrityViolationException) {
            // Two concurrent confirms: the unique source_message_id let only one insert win. Return that one.
            intents.findBySourceMessageId(messageId) ?: throw e
        }

    private fun createOnce(tenantId: UUID, conversationId: UUID, messageId: Long): Intent {
        conversations.requireOwned(tenantId, conversationId)
        val latest = messages.findFirstByConversationIdAndDraftIsNotNullOrderByIdDesc(conversationId)
        if (latest?.id != messageId) throw DraftNotConfirmableException("message $messageId is not the latest draft in this conversation")
        intents.findBySourceMessageId(messageId)?.let { return it }

        return intents.save(
            Intent(
                tenantId = tenantId,
                spec = objectMapper.convertValue(latest.draft!!),
                conversationId = conversationId,
                sourceMessageId = messageId,
            )
        )
    }
}
