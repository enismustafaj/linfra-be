package ai.linfra.resource_app.chat.domain.api.impl

import ai.linfra.resource_app.chat.domain.api.CreateThreadUseCase
import ai.linfra.resource_app.chat.domain.model.Thread
import ai.linfra.resource_app.chat.persistence.ThreadEntity
import ai.linfra.resource_app.chat.persistence.ThreadRepository
import org.springframework.stereotype.Component

@Component
class CreateThreadImpl(
    private val threadRepository: ThreadRepository
): CreateThreadUseCase {

    override fun createThread(threadName: String): Thread {
        val threadEntity = createThreadEntity(threadName)
        val savedEntity: ThreadEntity =  threadRepository.save(threadEntity)

        return createThreadModel(savedEntity)
    }

    private fun createThreadModel(threadEntity: ThreadEntity): Thread  {
        return Thread(threadId = threadEntity.threadId.toString(), threadName = threadEntity.threadName, prompts = emptyList(), userId = threadEntity.userId)
    }

    private fun createThreadEntity(threadName: String): ThreadEntity  {
        return ThreadEntity(threadName = threadName, userId = "dummyUser")
    }
}