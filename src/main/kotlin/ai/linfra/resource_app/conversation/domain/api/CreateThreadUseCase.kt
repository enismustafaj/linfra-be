package ai.linfra.resource_app.conversation.domain.api

import ai.linfra.resource_app.conversation.domain.model.ChatThread

interface CreateThreadUseCase {
    fun createThread(threadName: String): ChatThread
}