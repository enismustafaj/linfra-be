package ai.linfra.resource_app.conversation.persistence

data class ThreadPayload(
    val chats: MutableList<Chat>,
)

data class Chat(
    val role: String,
    val text: String
)