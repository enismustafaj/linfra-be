package ai.linfra.resource_app.ai_model.client

import ai.linfra.resource_app.ai_model.configuration.AIModelProperties
import ai.linfra.resource_app.chat.domain.dependencies.AiConversationModel
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Component
@EnableConfigurationProperties(AIModelProperties::class)
class AiConversationModelClientImpl(
    val aiModelProperties: AIModelProperties
): AiConversationModel {

    private val client: HttpClient = HttpClient.newHttpClient()

    private val completionEndpoint = "v1/chat/completions"

    override fun generateResponse(prompt: String): String {
        val httpRequest: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(aiModelProperties.url + completionEndpoint))
            .header("Authorization", "Bearer " + aiModelProperties.apiKey)
            .header("Content-Type", "application/json")
            .build()

        return client.send(httpRequest, HttpResponse.BodyHandlers.ofString()).body()
    }

}