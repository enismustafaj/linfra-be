package ai.linfra.resource_app.conversation.domain.api.impl

import ai.linfra.resource_app.ai_model.client.AiModelClient
import ai.linfra.resource_app.conversation.domain.api.UpdateThreadUseCase
import ai.linfra.resource_app.conversation.domain.model.ChatPrompt
import ai.linfra.resource_app.conversation.persistence.Chat
import ai.linfra.resource_app.conversation.persistence.ConversationThreadRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class UpdateThreadImpl(
    private val aiModelClient: AiModelClient,
    private val conversationThreadRepository: ConversationThreadRepository
): UpdateThreadUseCase {

    override fun updateThread(threadId: String, prompt: ChatPrompt): ChatPrompt {
        val thread = conversationThreadRepository.findById(UUID.fromString(threadId))
            .orElseThrow { IllegalArgumentException("Thread not found") }

        val aiResponse = aiModelClient.generateResponse(prompt.text)

        thread.threadPayload.chats.addLast(Chat(role = prompt.role, text = prompt.text))
        thread.threadPayload.chats.addLast(Chat(role = "AI", text = aiResponse))

        conversationThreadRepository.save(thread)

        return ChatPrompt(role = "AI", text = aiResponse)
    }
}