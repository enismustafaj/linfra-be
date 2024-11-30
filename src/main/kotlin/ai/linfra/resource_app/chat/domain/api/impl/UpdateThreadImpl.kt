package ai.linfra.resource_app.chat.domain.api.impl

import ai.linfra.resource_app.chat.domain.api.UpdateThreadUseCase
import ai.linfra.resource_app.chat.domain.dependencies.AiConversationModel
import ai.linfra.resource_app.chat.domain.model.ChatPrompt
import ai.linfra.resource_app.chat.domain.model.ChatRoleEnum
import ai.linfra.resource_app.chat.persistence.ChatThreadEntity
import ai.linfra.resource_app.chat.persistence.ChatThreadRepository
import ai.linfra.resource_app.chat.persistence.ThreadEntity
import ai.linfra.resource_app.chat.persistence.ThreadRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class UpdateThreadImpl(
    private val aiModelClient: AiConversationModel,
    private val threadRepository: ThreadRepository,
    private val chatThreadRepository: ChatThreadRepository
): UpdateThreadUseCase {

    override fun updateThread(threadId: String, prompt: ChatPrompt): ChatPrompt {
        val thread = threadRepository.findById(UUID.fromString(threadId))
            .orElseThrow { IllegalArgumentException("Thread not found") }

        val aiResponse = aiModelClient.generateResponse(prompt.text)

        val userChatThread: ChatThreadEntity = createChatThreadEntity(prompt, thread)
        val aiChatThread: ChatThreadEntity = createChatThreadEntity(ChatPrompt(ChatRoleEnum.AI_ASSISTANT, aiResponse), thread)

        chatThreadRepository.saveAll(listOf(userChatThread, aiChatThread))

        return ChatPrompt(role = ChatRoleEnum.AI_ASSISTANT, text = aiResponse)
    }

    private fun createChatThreadEntity(prompt: ChatPrompt, thread: ThreadEntity): ChatThreadEntity {
        return ChatThreadEntity(
            role = prompt.role,
            content = prompt.text,
            threadId = thread
        )
    }
}