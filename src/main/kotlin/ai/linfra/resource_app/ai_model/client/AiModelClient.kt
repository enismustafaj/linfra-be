package ai.linfra.resource_app.ai_model.client

interface AiModelClient {
    fun generateResponse(prompt: String): String
}