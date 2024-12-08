package ai.linfra.resource_app.chat.domain.api.impl

import ai.linfra.resource_app.chat.domain.api.GetThreadUseCase
import ai.linfra.resource_app.chat.domain.model.ChatPrompt
import ai.linfra.resource_app.chat.domain.model.Thread
import ai.linfra.resource_app.chat.persistence.ChatThreadEntity
import ai.linfra.resource_app.chat.persistence.ChatThreadRepository
import ai.linfra.resource_app.chat.persistence.ThreadEntity
import ai.linfra.resource_app.chat.persistence.ThreadRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class GetThreadImpl(
    private val threadRepository: ThreadRepository,
    private val chatThreadRepository: ChatThreadRepository
): GetThreadUseCase {

    override fun getThread(threadId: String): Thread {
        val threadEntity = threadRepository.findById(UUID.fromString(threadId)).orElseThrow { IllegalArgumentException("Chat thread not found") }
        val chats: List<ChatThreadEntity> = chatThreadRepository.findByThreadId(threadEntity)
        return toThread(threadEntity, chats)
    }

    override fun getThreads(): List<Thread> {
        return threadRepository.findAll().map { threadEntity ->
            toThread(threadEntity, listOf())
        }
    }

    private fun toThread(entity: ThreadEntity, chats: List<ChatThreadEntity>): Thread {
        return Thread(
            entity.threadId.toString(),
            entity.threadName,
            entity.userId.toString(),
            chats.map { ChatPrompt(it.role, it.content) }
        )
    }
}