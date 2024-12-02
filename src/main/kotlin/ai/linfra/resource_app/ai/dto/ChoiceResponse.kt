package ai.linfra.resource_app.ai.dto

data class ChoiceResponse(
    val finishReason: String,
    val index: Int,
    val message: MessagePayload
)
