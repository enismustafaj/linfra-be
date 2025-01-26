package ai.linfra.resource_app.resource.rest.gateway

import ai.linfra.resource_app.resource.rest.dto.ResourceSecretsDto
import ai.linfra.resource_app.resource.security.domain.api.DecryptResourceSecretsUseCase
import com.google.gson.JsonObject
import org.springframework.stereotype.Component

@Component
class ResourceSecretsGateway(
    val decryptResourceSecretsUseCase: DecryptResourceSecretsUseCase
) {

    fun getResourceSecrets(resourceId: String): ResourceSecretsDto {
        val secrets: JsonObject = decryptResourceSecretsUseCase.decryptResourceSecrets(resourceId)
        return  ResourceSecretsDto(secrets)
    }
}