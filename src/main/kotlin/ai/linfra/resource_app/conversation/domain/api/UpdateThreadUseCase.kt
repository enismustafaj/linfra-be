package ai.linfra.resource_app.conversation.domain.api

import ai.linfra.resource_app.conversation.domain.model.ChatPrompt

interface UpdateThreadUseCase {
    fun updateThread(threadId: String, prompt: ChatPrompt): ChatPrompt
}