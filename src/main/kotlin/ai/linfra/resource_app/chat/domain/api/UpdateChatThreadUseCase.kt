package ai.linfra.resource_app.chat.domain.api

import ai.linfra.resource_app.chat.domain.model.ChatPrompt

interface UpdateChatThreadUseCase {
    fun updateThreadConversations(threadId: String, prompt: ChatPrompt): List<ChatPrompt>
}