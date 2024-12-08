package ai.linfra.resource_app.chat.domain.command

data class UpdateChatMetadataCommand(
    val chatId: String,
    val action: String,
    val reference: String
)
