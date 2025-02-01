package ai.linfra.resource_app.chat.rest

import ai.linfra.resource_app.chat.domain.api.CreateThreadUseCase
import ai.linfra.resource_app.chat.domain.api.GetThreadUseCase
import ai.linfra.resource_app.chat.domain.api.UpdateThreadUseCase
import ai.linfra.resource_app.chat.domain.model.Thread
import ai.linfra.resource_app.chat.rest.dto.ChatOverviewResDto
import ai.linfra.resource_app.chat.rest.dto.ChatThreadsResDto
import ai.linfra.resource_app.chat.rest.dto.ThreadReqDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
class ThreadController(
    val updateThreadUseCase: UpdateThreadUseCase,
    val createThreadUseCase: CreateThreadUseCase,
    val getThreadUseCase: GetThreadUseCase,
) {

    @PostMapping
    fun createThread(@RequestBody threadDto: ThreadReqDto): ResponseEntity<ChatThreadsResDto> {
        val chatThread = createThreadUseCase.createThread(threadDto.threadName)
        return ResponseEntity.created(URI("/threads/" + chatThread.threadId)).body(
            ChatThreadsResDto(
            chatThread.threadId,
            chatThread.threadName
        )
        )
    }

    @PutMapping("/{threadId}")
    fun updateThread(@PathVariable threadId: String, @RequestBody threadDto: ThreadReqDto): ChatThreadsResDto {
        val thread: Thread = updateThreadUseCase.updateThreadName(threadId, threadDto.threadName)

        return ChatThreadsResDto(
            chatThreadId = thread.threadId,
            chatThreadName =  thread.threadName
        )
    }

    @GetMapping
    fun getThreads(): ChatOverviewResDto {
        val chatThreads = getThreadUseCase.getThreads()
        return ChatOverviewResDto(
            chatThreads.map { thread -> ChatThreadsResDto(thread.threadId, thread.threadName) }
        )
    }
}