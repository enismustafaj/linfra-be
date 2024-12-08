package ai.linfra.resource_app.resource.rest

import ai.linfra.resource_app.resource.domain.api.GetResourceUseCase
import ai.linfra.resource_app.resource.domain.api.ResourceCreationUseCase
import ai.linfra.resource_app.resource.domain.api.ResourceUpdateUseCase
import ai.linfra.resource_app.resource.domain.model.ResourceDescription
import ai.linfra.resource_app.resource.domain.model.ResourceProperty
import ai.linfra.resource_app.resource.domain.model.ResourceTypeEnum
import ai.linfra.resource_app.resource.rest.dto.ResourcePropertyDto
import ai.linfra.resource_app.resource.rest.dto.ResourceReqDto
import ai.linfra.resource_app.resource.rest.dto.ResourceResDto
import org.springframework.stereotype.Component

@Component
class ResourceGateway(
    private val resourceCreationUseCase: ResourceCreationUseCase,
    private val getResourceUseCase: GetResourceUseCase,
    private val resourceUpdateUseCase: ResourceUpdateUseCase
) {

    fun createResource(resource: ResourceReqDto): ResourceResDto {
        val createdResource = resourceCreationUseCase.createResource(resource.chatId, toResourceDescription(resource))
        return ResourceResDto(createdResource.resourceId!!, createdResource.resourceType.name, createdResource.resourceType.name, toResourcePropertyDto(createdResource.resourceProperties))
    }

    fun getResource(resourceId: String): ResourceResDto {
        val resource = getResourceUseCase.getResource(resourceId)
        return ResourceResDto(resource.resourceId!!, resource.resourceType.name, resource.resourceName, toResourcePropertyDto(resource.resourceProperties))
    }

    fun updateResource(resourceId: String, resource: ResourceReqDto): ResourceResDto {
        val updatedResource = resourceUpdateUseCase.updateResource(resource.chatId, resourceId, toResourceDescription(resource))
        return ResourceResDto(updatedResource.resourceId!!, updatedResource.resourceType.name, updatedResource.resourceType.name, toResourcePropertyDto(updatedResource.resourceProperties))
    }

    private fun toResourcePropertyDto(resourceProperties: List<ResourceProperty>): List<ResourcePropertyDto> {
        return resourceProperties.map {
            ResourcePropertyDto(it.propertyName, it.propertyValue)
        }
    }

    private fun toResourceDescription(resource: ResourceReqDto): ResourceDescription {
        return ResourceDescription(
            resourceName = resource.name,
            resourceType = ResourceTypeEnum.valueOf(resource.resourceType),
            resourceProperties = toResourceProperty(resource.properties)
        )
    }

    private fun toResourceProperty(resourceProperties: List<ResourcePropertyDto>): List<ResourceProperty> {
        return resourceProperties.map {
            ResourceProperty(it.name, it.value)
        }
    }
}