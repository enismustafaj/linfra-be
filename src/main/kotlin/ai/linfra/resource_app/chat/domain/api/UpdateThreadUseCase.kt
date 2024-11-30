package ai.linfra.resource_app.chat.domain.api

import ai.linfra.resource_app.chat.domain.model.ChatPrompt

interface UpdateThreadUseCase {
    fun updateThread(threadId: String, prompt: ChatPrompt): ChatPrompt
}