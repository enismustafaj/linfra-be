package ai.linfra.resource_app.chat.domain.model

data class Thread(
    val threadId: String,
    val userId: String,
    val threadName: String,
    val prompts: List<ChatPrompt>
)
