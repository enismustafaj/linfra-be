package ai.linfra.resource_app.ai.model

data class ChatCompletionResponse(
    val choices: List<ChoiceResponse>,
    val model: String,
) {
}