package ai.linfra.resource_app.conversation.domain.api.impl

import ai.linfra.resource_app.conversation.domain.api.GetThreadUseCase
import ai.linfra.resource_app.conversation.domain.model.ChatPrompt
import ai.linfra.resource_app.conversation.domain.model.ChatThread
import ai.linfra.resource_app.conversation.persistence.ConversationThreadEntity
import ai.linfra.resource_app.conversation.persistence.ConversationThreadRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class GetThreadImpl(
    private val threadRepository: ConversationThreadRepository
): GetThreadUseCase {

    override fun getThread(threadId: String): ChatThread {
        val threadEntity = threadRepository.findById(UUID.fromString(threadId)).orElseThrow { IllegalArgumentException("Chat thread not found") }
        return toChatThread(threadEntity)
    }

    private fun toChatThread(entity: ConversationThreadEntity): ChatThread {
        return ChatThread(
            entity.id.toString(),
            entity.threadName,
            entity.threadPayload.chats.map { ChatPrompt(it.role, it.text) }
        )
    }
}