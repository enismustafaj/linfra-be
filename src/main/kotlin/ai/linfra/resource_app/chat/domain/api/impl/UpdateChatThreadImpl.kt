package ai.linfra.resource_app.chat.domain.api.impl

import ai.linfra.resource_app.chat.domain.api.UpdateChatThreadUseCase
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
class UpdateChatThreadImpl(
    private val aiModelClient: AiConversationModel,
    private val threadRepository: ThreadRepository,
    private val chatThreadRepository: ChatThreadRepository
): UpdateChatThreadUseCase {

    private val initSystemText: String = """
        You are a infrastructure resource assistant! The only resource you can create is databases of types MySQL and PostgreSQL. 
        The properties of the database that you are interested in are the name, the type and the instance size. The instance size can be micro, small or medium.
    """

    @Transactional
    override fun updateThreadConversations(threadId: String, prompt: ChatPrompt): List<ChatPrompt> {
        val thread = threadRepository.findById(UUID.fromString(threadId))
            .orElseThrow { IllegalArgumentException("Thread not found") }

        val userChatThread: ChatThreadEntity = chatThreadRepository.save(createChatThreadEntity(prompt, thread))

        val aiResponse = aiModelClient.generateResponse(getChatContext(threadId))

        val aiChatThread: ChatThreadEntity = chatThreadRepository.save(createChatThreadEntity(ChatPrompt(role = ChatRoleEnum.assistant, content = aiResponse), thread))

        return listOf(
            ChatPrompt(id = userChatThread.chatId.toString(), role = ChatRoleEnum.user, content = prompt.content),
            ChatPrompt(id = aiChatThread.chatId.toString(), role = ChatRoleEnum.assistant, content = aiResponse)
        )
    }

    private fun getChatContext(threadId: String): List<ChatPrompt> {
        val chats: MutableList<ChatPrompt> = mutableListOf(ChatPrompt(role = ChatRoleEnum.system, content = initSystemText))
        val thread: ThreadEntity = threadRepository.findById(UUID.fromString(threadId)).get()
        chats.addAll(chatThreadRepository.findByThreadIdOrderByCreatedAtAsc(thread).map {
            ChatPrompt(id = it.chatId.toString(), role = it.role, content = it.content)
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