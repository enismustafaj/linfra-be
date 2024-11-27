package ai.linfra.resource_app.conversation.domain.api.impl

import ai.linfra.resource_app.conversation.domain.api.CreateThreadUseCase
import ai.linfra.resource_app.conversation.domain.model.ChatThread
import ai.linfra.resource_app.conversation.persistence.ConversationThreadEntity
import ai.linfra.resource_app.conversation.persistence.ConversationThreadRepository
import org.springframework.stereotype.Component

@Component
class CreateThreadImpl(
    private val threadRepository: ConversationThreadRepository
): CreateThreadUseCase {

    override fun createThread(threadName: String): ChatThread {
        val threadEntity = createThreadEntity(threadName)
        val savedEntity: ConversationThreadEntity =  threadRepository.save(threadEntity)

        return createThreadModel(savedEntity)
    }

    private fun createThreadModel(threadEntity: ConversationThreadEntity): ChatThread  {
        return ChatThread(threadId = threadEntity.id.toString(), threadName = threadEntity.threadName, prompts = emptyList())
    }

    private fun createThreadEntity(threadName: String): ConversationThreadEntity  {
        return ConversationThreadEntity(threadName = threadName, userId = "dummyUser")
    }
}