package ai.linfra.resource_app.chat.domain.model

data class ChatPrompt(
    val id: String? = null,
    val role: ChatRoleEnum,
    val content: String,
    val metadata: ChatMetadata? = null
)
