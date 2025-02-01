package ai.linfra.resource_app.chat.domain.api

import ai.linfra.resource_app.chat.domain.model.Thread

interface UpdateThreadUseCase {
    fun updateThreadName(threadId: String, threadName: String): Thread
}