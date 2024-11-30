package ai.linfra.resource_app.chat.rest

import ai.linfra.resource_app.chat.domain.api.CreateThreadUseCase
import ai.linfra.resource_app.chat.domain.api.GetThreadUseCase
import ai.linfra.resource_app.chat.domain.api.UpdateThreadUseCase
import ai.linfra.resource_app.chat.domain.model.ChatPrompt
import ai.linfra.resource_app.chat.rest.dto.ChatOverviewResDto
import ai.linfra.resource_app.chat.rest.dto.PromptDto
import ai.linfra.resource_app.chat.rest.dto.ThreadReqDto
import ai.linfra.resource_app.chat.rest.dto.ThreadResDto
import lombok.RequiredArgsConstructor
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequiredArgsConstructor
class ConversationController(
    private val createThreadUseCase: CreateThreadUseCase,
    private val getThreadUseCase: GetThreadUseCase,
    private val updateThreadUseCase: UpdateThreadUseCase
) {

    @PostMapping("/threads")
    fun createThread(@RequestBody threadDto: ThreadReqDto): ResponseEntity<ThreadResDto> {
        val chatThread = createThreadUseCase.createThread(threadDto.threadName)
        return ResponseEntity.created(URI("/threads/" + chatThread.threadId)).body(ThreadResDto(
            chatThread.threadId,
            chatThread.threadName,
            chatThread.prompts.map { PromptDto(it.role, it.text) }
        ))
    }

    @PutMapping("/threads/{threadId}")
    fun getResponse(@RequestBody promptDto: PromptDto, @PathVariable threadId: String): PromptDto  {
        val aiPrompt: ChatPrompt = updateThreadUseCase.updateThread(threadId, ChatPrompt(promptDto.role, promptDto.text))
        return PromptDto(aiPrompt.role, aiPrompt.text)
    }

    @GetMapping("/threads")
    fun getThreads(): ChatOverviewResDto {
        val chatThreads = getThreadUseCase.getThreads()
        return ChatOverviewResDto(
            chatThreads.map { thread -> ThreadResDto(thread.threadId, thread.threadName, thread.prompts.map {  PromptDto(it.role, it.text) }) }
        )
    }

    @GetMapping("/threads/{threadId}")
    fun getThread(@PathVariable threadId: String): ThreadResDto {
        val chatThread = getThreadUseCase.getThread(threadId)
        return ThreadResDto(
            chatThread.threadId,
            chatThread.threadName,
            chatThread.prompts.map { PromptDto(it.role, it.text) }
        )
    }
}