package ai.linfra.resource_app.ai.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessagePayload(
    val role: String,
    val content: String
)
