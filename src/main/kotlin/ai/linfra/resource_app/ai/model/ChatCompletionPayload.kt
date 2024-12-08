package ai.linfra.resource_app.ai.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionPayload(
    val model: String,
    val messages: List<MessagePayload>,
    val stream: Boolean
)
