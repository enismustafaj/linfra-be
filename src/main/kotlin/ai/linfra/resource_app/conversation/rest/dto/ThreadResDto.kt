package ai.linfra.resource_app.conversation.rest.dto

data class ThreadResDto(
    val threadId: String,
    val threadName: String,
    val prompts: List<PromptDto>
)
