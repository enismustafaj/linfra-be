package ai.linfra.resource_app.ai.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionResponse(
    val choices: List<ChoiceResponse>,
    val model: String,
) {
}