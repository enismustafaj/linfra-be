package ai.linfra.resource_app.conversation.rest

import ai.linfra.resource_app.conversation.domain.api.CreateThreadUseCase
import ai.linfra.resource_app.conversation.domain.api.GetThreadUseCase
import ai.linfra.resource_app.conversation.rest.dto.PromptDto
import ai.linfra.resource_app.conversation.rest.dto.ThreadReqDto
import ai.linfra.resource_app.conversation.rest.dto.ThreadResDto
import lombok.RequiredArgsConstructor
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@RequiredArgsConstructor
class ConversationController(
    private val createThreadUseCase: CreateThreadUseCase,
    private val getThreadUseCase: GetThreadUseCase
) {

    @PostMapping("/threads")
    fun createThread(@RequestBody threadDto: ThreadReqDto): ThreadResDto {
        val chatThread = createThreadUseCase.createThread(threadDto.threadName)
        return ThreadResDto(
            chatThread.threadId,
            chatThread.threadName,
            chatThread.prompts.map { PromptDto(it.role, it.text) }
        )
    }

    @PutMapping("/threads/{threadId}")
    fun getResponse(@RequestBody promptDto: PromptDto, @PathVariable threadId: String): PromptDto  {
        return PromptDto("model", "response")
    }

    @GetMapping("/threads")
    fun getThreads() {

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