package ai.linfra.resource_app.chat.domain.api

import ai.linfra.resource_app.chat.domain.model.Thread

interface GetThreadUseCase {
    fun getThread(threadId: String): Thread

    fun getThreads(): List<Thread>
}