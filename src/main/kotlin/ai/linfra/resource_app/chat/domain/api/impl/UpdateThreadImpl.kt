package ai.linfra.resource_app.chat.domain.api.impl

import ai.linfra.resource_app.chat.domain.api.UpdateThreadUseCase
import ai.linfra.resource_app.chat.domain.dependencies.AiConversationModel
import ai.linfra.resource_app.chat.domain.model.ChatPrompt
import ai.linfra.resource_app.chat.domain.model.ChatRoleEnum
import ai.linfra.resource_app.chat.persistence.ChatThreadEntity
import ai.linfra.resource_app.chat.persistence.ChatThreadRepository
import ai.linfra.resource_app.chat.persistence.ThreadEntity
import ai.linfra.resource_app.chat.persistence.ThreadRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import java.util.*

@Component
class UpdateThreadImpl(
    private val aiModelClient: AiConversationModel,
    private val threadRepository: ThreadRepository,
    private val chatThreadRepository: ChatThreadRepository
): UpdateThreadUseCase {

    private val initSystemText: String = """
        Hi! I am an AI assistant. I can help you with your queries. 
        Please ask me anything.
    """

    @Transactional
    override fun updateThread(threadId: String, prompt: ChatPrompt): ChatPrompt {
        val thread = threadRepository.findById(UUID.fromString(threadId))
            .orElseThrow { IllegalArgumentException("Thread not found") }

        val userChatThread: ChatThreadEntity = createChatThreadEntity(prompt, thread)
        chatThreadRepository.save(userChatThread)

        val aiResponse = aiModelClient.generateResponse(getChatContext(threadId))

        val aiChatThread: ChatThreadEntity = createChatThreadEntity(ChatPrompt(ChatRoleEnum.assistant, aiResponse), thread)
        chatThreadRepository.save(aiChatThread)

        return ChatPrompt(role = ChatRoleEnum.assistant, content = aiResponse)
    }

    private fun getChatContext(threadId: String): List<ChatPrompt> {
        val chats: MutableList<ChatPrompt> = mutableListOf(ChatPrompt(ChatRoleEnum.system, initSystemText))
        val thread: ThreadEntity = threadRepository.findById(UUID.fromString(threadId)).get()
        chats.addAll(chatThreadRepository.findByThreadIdOrderByCreatedAtAsc(thread).map {
            ChatPrompt(it.role, it.content)
        })

        return chats
    }

    private fun createChatThreadEntity(prompt: ChatPrompt, thread: ThreadEntity): ChatThreadEntity {
        return ChatThreadEntity(
            role = prompt.role,
            content = prompt.content,
            threadId = thread
        )
    }
}