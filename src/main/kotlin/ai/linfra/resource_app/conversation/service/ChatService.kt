package ai.linfra.resource_app.conversation.service

import ai.linfra.resource_app.conversation.exception.InvalidAgentOutputException
import ai.linfra.resource_app.conversation.exception.InvalidMessageException
import ai.linfra.resource_app.conversation.model.MessageRole
import ai.linfra.resource_app.conversation.persistence.Message
import ai.linfra.resource_app.conversation.persistence.MessageRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

private const val MAX_MESSAGE_CHARS = 2000

/** One exchange: store the user's message, ask the agent, store the assistant's reply. */
@Service
class ChatService(
    private val conversations: ConversationService,
    private val messages: MessageRepository,
    private val agent: AgentClient,
    private val drafts: DraftBuilder,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Not @Transactional on purpose: the user message commits, then the LLM is called with no transaction open
     * (a hung LLM holds no DB connection), then the reply commits. A crash in between loses only the reply,
     * and LLM calls have no side effects, so the user can simply resend.
     */
    fun send(tenantId: UUID, conversationId: UUID, text: String): Message {
        if (text.isBlank() || text.length > MAX_MESSAGE_CHARS) throw InvalidMessageException("text must be 1..$MAX_MESSAGE_CHARS characters")
        conversations.requireOwned(tenantId, conversationId)
        messages.save(Message(conversationId = conversationId, role = MessageRole.USER, content = text))

        val agentReply = try {
            agent.reply(conversations.history(conversationId))
        } catch (e: InvalidAgentOutputException) {
            log.warn("Invalid agent output in conversation {}: {}", conversationId, e.message)
            null
        }
        val reply = drafts.build(agentReply)
        return messages.save(Message(conversationId = conversationId, role = MessageRole.ASSISTANT, content = reply.content, draft = reply.draft))
    }
}
