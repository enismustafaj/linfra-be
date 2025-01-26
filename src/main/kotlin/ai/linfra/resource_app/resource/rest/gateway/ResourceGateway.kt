package ai.linfra.resource_app.resource.rest.gateway

import ai.linfra.resource_app.resource.domain.api.DeleteResourceUseCase
import ai.linfra.resource_app.resource.domain.api.GetResourceUseCase
import ai.linfra.resource_app.resource.domain.api.ResourceCreationUseCase
import ai.linfra.resource_app.resource.domain.api.ResourceUpdateUseCase
import ai.linfra.resource_app.resource.domain.model.ResourceDescription
import ai.linfra.resource_app.resource.domain.model.ResourceProperty
import ai.linfra.resource_app.resource.domain.model.ResourceTypeEnum
import ai.linfra.resource_app.resource.rest.dto.ResourcePropertyDto
import ai.linfra.resource_app.resource.rest.dto.ResourceReqDto
import ai.linfra.resource_app.resource.rest.dto.ResourceResDto
import ai.linfra.resource_app.resource.rest.dto.ResourcesResDto
import org.springframework.stereotype.Component

@Component
class ResourceGateway(
    private val resourceCreationUseCase: ResourceCreationUseCase,
    private val getResourceUseCase: GetResourceUseCase,
    private val resourceUpdateUseCase: ResourceUpdateUseCase,
    private val deleteResourceUseCase: DeleteResourceUseCase
) {

    fun createResource(resource: ResourceReqDto): ResourceResDto {
        val createdResource: ResourceDescription = resourceCreationUseCase.createResource(resource.chatId, toResourceDescription(resource))
        return toResourceResDto(createdResource)
    }

    fun getResource(resourceId: String): ResourceResDto {
        val resource: ResourceDescription = getResourceUseCase.getResource(resourceId)
        return toResourceResDto(resource)
    }

    fun updateResource(resourceId: String, resource: ResourceReqDto): ResourceResDto {
        val updatedResource: ResourceDescription = resourceUpdateUseCase.updateResource(resource.chatId, resourceId, toResourceDescription(resource))
        return toResourceResDto(updatedResource)
    }

    fun deleteResource(chatId: String, resourceId: String) {
        deleteResourceUseCase.deleteResource(chatId, resourceId)
    }

    fun getResourcesForUser(): ResourcesResDto {
        // TODO: get the userid from the token
        val resources: List<ResourceDescription> = getResourceUseCase.getResourcesForUser("")

        return ResourcesResDto(resources.map{
            ResourceResDto(it.resourceId.toString(), it.resourceName, it.chatThreadId, it.resourceType.name, it.resourceStatus!!.name, toResourcePropertyDto(it.resourceProperties))
        })
    }

    fun getResourceForChatThreadId(chatThreadId: String): ResourceResDto {
        val resource: ResourceDescription = getResourceUseCase.getResourceForChatThreadId(chatThreadId)
        return toResourceResDto(resource)
    }

    private fun toResourceResDto(resource: ResourceDescription): ResourceResDto {
        return ResourceResDto(resource.resourceId!!, resource.resourceName, resource.chatThreadId, resource.resourceType.name, resource.resourceStatus!!.name, toResourcePropertyDto(resource.resourceProperties))
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
            resourceProperties = toResourceProperty(resource.properties),
            chatThreadId = resource.chatThreadId
        )
    }

    private fun toResourceProperty(resourceProperties: List<ResourcePropertyDto>): List<ResourceProperty> {
        return resourceProperties.map {
            ResourceProperty(it.name, it.value)
        }
    }
}