package ai.linfra.resource_app.ai.dto

data class ChatCompletionResponse(
    val choices: List<ChoiceResponse>,
    val model: String,
) {
}