package ai.linfra.resource_app.chat.domain.dependencies

interface AiConversationModel {
    fun generateResponse(prompt: String): String
}