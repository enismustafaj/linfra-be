package ai.linfra.resource_app.ai.client

import ai.linfra.resource_app.ai.configuration.AIModelProperties
import ai.linfra.resource_app.ai.model.ChatCompletionPayload
import ai.linfra.resource_app.ai.model.ChatCompletionResponse
import ai.linfra.resource_app.ai.model.MessagePayload
import ai.linfra.resource_app.chat.domain.dependencies.AiConversationModel
import ai.linfra.resource_app.chat.domain.model.ChatPrompt
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class AiConversationModelClientImpl(
    private val aiModelProperties: AIModelProperties
): AiConversationModel {

    private val client: HttpClient = HttpClient.newHttpClient()

    private val completionEndpoint: String = "v1/chat/completions"

    private val model: String = "grok-beta"

    override fun generateResponse(context: List<ChatPrompt>): String {
        val httpRequest: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(aiModelProperties.url + completionEndpoint))
            .POST(HttpRequest.BodyPublishers.ofString(Json.encodeToString(ChatCompletionPayload.serializer(), convertChatPromptToRequest(context))))
            .header("Authorization", "Bearer " + aiModelProperties.apiKey)
            .header("Content-Type", "application/json")
            .build()

        val response: ChatCompletionResponse = Json.decodeFromString<ChatCompletionResponse>(client.send(httpRequest, HttpResponse.BodyHandlers.ofString()).body())

        return response.choices[0].message.content
    }

    private fun convertChatPromptToRequest(context: List<ChatPrompt>): ChatCompletionPayload {
        return  ChatCompletionPayload(model = model, stream = false, messages = context.map { MessagePayload(it.role.name, it.content) })
    }
}