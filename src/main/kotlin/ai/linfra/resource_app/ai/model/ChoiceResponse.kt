package ai.linfra.resource_app.ai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChoiceResponse(
    @SerialName("finish_reason") val finishReason: String,
    val index: Int,
    val message: MessagePayload
)
