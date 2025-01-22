package ai.linfra.resource_app.resource.rest

import ai.linfra.resource_app.resource.rest.dto.ResourceSecretsDto
import ai.linfra.resource_app.resource.rest.gateway.ResourceSecretsGateway
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/resources")
class ResourceSecretsController(
    val resourceSecretsGateway: ResourceSecretsGateway
) {

    @GetMapping("/{resourceId}/secrets")
    fun getResourceSecrets(@PathVariable resourceId: String): ResourceSecretsDto {
        return resourceSecretsGateway.getResourceSecrets(resourceId)
    }
}