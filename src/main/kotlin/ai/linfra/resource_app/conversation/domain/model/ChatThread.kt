package ai.linfra.resource_app.conversation.domain.model

data class ChatThread(
    val threadId: String,
    val threadName: String,
    val prompts: List<ChatPrompt>
)
