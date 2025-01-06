package ai.linfra.resource_app.resource.rest

import org.springframework.stereotype.Component

import ai.linfra.resource_app.resource.domain.api.ResourceProcessUpdateUseCase
import ai.linfra.resource_app.resource.domain.model.ResourceDescription
import ai.linfra.resource_app.resource.domain.model.ResourceProperty
import ai.linfra.resource_app.resource.rest.dto.ResourcePropertyDto
import ai.linfra.resource_app.resource.rest.dto.ResourceResDto

@Component
class ResourceNotificationGateway(
    val resourceUpdateUseCase: ResourceProcessUpdateUseCase
) {

    fun updateResourceStatus(resourceId: String, status: String): ResourceResDto {
        val resource: ResourceDescription = resourceUpdateUseCase.updateResourceStatus(resourceId, status)

        return ResourceResDto(resource.resourceId!!, resource.resourceType.name, resource.resourceName, toResourcePropertyDto(resource.resourceProperties))
    }


    private fun toResourcePropertyDto(resourceProperties: List<ResourceProperty>): List<ResourcePropertyDto> {
        return resourceProperties.map {
            ResourcePropertyDto(it.propertyName, it.propertyValue)
        }
    }

}