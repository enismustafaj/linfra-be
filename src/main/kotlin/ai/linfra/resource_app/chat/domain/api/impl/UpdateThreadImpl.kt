package ai.linfra.resource_app.chat.domain.api.impl

import ai.linfra.resource_app.chat.domain.api.UpdateThreadUseCase
import ai.linfra.resource_app.chat.domain.model.Thread
import ai.linfra.resource_app.chat.persistence.ThreadEntity
import ai.linfra.resource_app.chat.persistence.ThreadRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UpdateThreadImpl(
    val threadRepository: ThreadRepository
): UpdateThreadUseCase {

    override fun updateThreadName(threadId: String, threadName: String): Thread {
        val thread: ThreadEntity = threadRepository.findById(UUID.fromString(threadId)).orElseThrow()
        thread.threadName = threadName

        val savedThread: ThreadEntity = threadRepository.save(thread)

        return Thread(
            threadId = savedThread.threadId!!.toString(),
            userId = savedThread.userId.toString(),
            threadName = savedThread.threadName,
            prompts = listOf()
        )
    }
}