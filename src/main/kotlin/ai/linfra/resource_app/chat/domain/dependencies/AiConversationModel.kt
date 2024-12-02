package ai.linfra.resource_app.chat.domain.dependencies

import ai.linfra.resource_app.chat.domain.model.ChatPrompt

interface AiConversationModel {
    fun generateResponse(context: List<ChatPrompt>): String
}