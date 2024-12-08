package ai.linfra.resource_app.chat.rest.dto

data class ThreadResDto(
    val threadId: String,
    val threadName: String,
    val prompts: List<ChatPromptDto>
)
