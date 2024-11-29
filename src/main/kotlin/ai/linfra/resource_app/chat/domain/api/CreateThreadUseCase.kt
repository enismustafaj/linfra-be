package ai.linfra.resource_app.chat.domain.api

import ai.linfra.resource_app.chat.domain.model.Thread

interface CreateThreadUseCase {
    fun createThread(threadName: String): Thread
}