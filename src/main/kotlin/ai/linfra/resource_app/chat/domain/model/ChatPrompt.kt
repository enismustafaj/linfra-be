package ai.linfra.resource_app.chat.domain.model

data class ChatPrompt(
    val role: ChatRoleEnum,
    val text: String
)
