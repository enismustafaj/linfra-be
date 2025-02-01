package ai.linfra.resource_app.chat.rest

import ai.linfra.resource_app.chat.domain.api.GetThreadUseCase
import ai.linfra.resource_app.chat.domain.api.UpdateChatThreadUseCase
import ai.linfra.resource_app.chat.domain.model.ChatPrompt
import ai.linfra.resource_app.chat.rest.dto.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/threads")
class ConversationController(
    private val getThreadUseCase: GetThreadUseCase,
    private val updateChatThreadUseCase: UpdateChatThreadUseCase,
) {

    @PutMapping("/{threadId}/conversations")
    fun getResponse(@RequestBody promptDto: ChatPromptDto, @PathVariable threadId: String): ChatResponseResDto  {
        val aiPrompt: List<ChatPrompt> = updateChatThreadUseCase.updateThreadConversations(threadId, ChatPrompt(role = promptDto.role, content = promptDto.text))
        return ChatResponseResDto(
            prompts = aiPrompt.map { prompt -> ChatPromptDto(prompt.id!!, prompt.role, prompt.content) }
        )
    }

    @GetMapping("/{threadId}/conversations")
    fun getThreadMessages(@PathVariable threadId: String): ThreadResDto {
        val chatThread = getThreadUseCase.getThread(threadId)
        return ThreadResDto(
            chatThread.prompts.map { ChatPromptDto(it.id!!, it.role, it.content) }
        )
    }
}