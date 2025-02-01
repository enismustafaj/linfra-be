package ai.linfra.resource_app.tfbe.client.impl

import ai.linfra.resource_app.tfbe.client.TfbeClient
import ai.linfra.resource_app.tfbe.client.dto.Attributes
import ai.linfra.resource_app.tfbe.client.dto.Data
import ai.linfra.resource_app.tfbe.client.dto.TfbeReqDto
import ai.linfra.resource_app.tfbe.config.TfbeConfiguration
import kotlinx.serialization.json.Json
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Component
@EnableConfigurationProperties(TfbeConfiguration::class)
class TfbeClientImpl(
    val httpClient: HttpClient = HttpClient.newHttpClient(),
    val tfbeConfiguration: TfbeConfiguration
): TfbeClient {

    internal val url: String = "https://app.terraform.io/api/v2/organizations/linfra_org/workspaces"

    override fun createtfbe(beId: String) {
        val response: HttpResponse<String> = httpClient.send(createRequest(beId), HttpResponse.BodyHandlers.ofString())
    }

    private fun createPayload(beId: String): TfbeReqDto {
        return TfbeReqDto(
            data = Data(
                type = "workspace",
                attributes = Attributes(
                    name = beId,
                    description = "Workspace for user $beId",
                    autoApply = true,
                    executionMode = "remote"
                )
            )
        )
    }

    private fun createRequest(beId: String): HttpRequest {
        return  HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer ${tfbeConfiguration.token}")
            .header("Content-Type", "application/vnd.api+json")
            .POST(HttpRequest.BodyPublishers.ofString(Json.encodeToString(TfbeReqDto.serializer(), createPayload(beId))))
            .build();
    }
}