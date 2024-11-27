package ai.linfra.resource_app.conversation.domain.api

import ai.linfra.resource_app.conversation.domain.model.ChatThread

interface GetThreadUseCase {
    fun getThread(threadId: String): ChatThread
}