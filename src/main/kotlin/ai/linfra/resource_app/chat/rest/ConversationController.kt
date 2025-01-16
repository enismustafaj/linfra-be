package ai.linfra.resource_app.chat.rest

import ai.linfra.resource_app.chat.domain.api.CreateThreadUseCase
import ai.linfra.resource_app.chat.domain.api.GetThreadUseCase
import ai.linfra.resource_app.chat.domain.api.UpdateThreadUseCase
import ai.linfra.resource_app.chat.domain.model.ChatPrompt
import ai.linfra.resource_app.chat.rest.dto.*
import lombok.RequiredArgsConstructor
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/threads")
class ConversationController(
    private val createThreadUseCase: CreateThreadUseCase,
    private val getThreadUseCase: GetThreadUseCase,
    private val updateThreadUseCase: UpdateThreadUseCase
) {

    @PostMapping
    fun createThread(@RequestBody threadDto: ThreadReqDto): ResponseEntity<ChatThreadsResDto> {
        val chatThread = createThreadUseCase.createThread(threadDto.threadName)
        return ResponseEntity.created(URI("/threads/" + chatThread.threadId)).body(ChatThreadsResDto(
            chatThread.threadId,
            chatThread.threadName
        ))
    }

    @PutMapping("/{threadId}")
    fun getResponse(@RequestBody promptDto: ChatPromptDto, @PathVariable threadId: String): ChatResponseResDto  {
        val aiPrompt: List<ChatPrompt> = updateThreadUseCase.updateThread(threadId, ChatPrompt(role = promptDto.role, content = promptDto.text))
        return ChatResponseResDto(
            prompts = aiPrompt.map { prompt -> ChatPromptDto(prompt.id!!, prompt.role, prompt.content) }
        )
    }

    @GetMapping
    fun getThreads(): ChatOverviewResDto {
        val chatThreads = getThreadUseCase.getThreads()
        return ChatOverviewResDto(
            chatThreads.map { thread -> ChatThreadsResDto(thread.threadId, thread.threadName) }
        )
    }

    @GetMapping("/{threadId}")
    fun getThread(@PathVariable threadId: String): ThreadResDto {
        val chatThread = getThreadUseCase.getThread(threadId)
        return ThreadResDto(
            chatThread.threadId,
            chatThread.threadName,
            chatThread.prompts.map { ChatPromptDto(it.id!!, it.role, it.content) }
        )
    }
}