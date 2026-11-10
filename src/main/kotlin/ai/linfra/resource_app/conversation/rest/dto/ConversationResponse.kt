package ai.linfra.resource_app.conversation.rest.dto

import java.util.UUID

data class ConversationResponse(
    val id: UUID,
    val messages: List<MessageResponse>,
    val intents: List<IntentSummary>,
)
