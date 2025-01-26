package ai.linfra.resource_app.resource.rest.gateway

import org.springframework.stereotype.Component

import ai.linfra.resource_app.resource.domain.api.ResourceRemoteUpdateUseCase
import ai.linfra.resource_app.resource.domain.model.ResourceDescription
import ai.linfra.resource_app.resource.domain.model.ResourceProperty
import ai.linfra.resource_app.resource.domain.model.ResourceStatusEnum
import ai.linfra.resource_app.resource.domain.model.command.ResourceRemoteUpdateCommand
import ai.linfra.resource_app.resource.rest.dto.ResourceNotificationDto
import ai.linfra.resource_app.resource.rest.dto.ResourcePropertyDto
import ai.linfra.resource_app.resource.rest.dto.ResourceResDto

@Component
class ResourceNotificationGateway(
    val resourceUpdateUseCase: ResourceRemoteUpdateUseCase
) {

    fun updateResourceStatus(resourceId: String, resourceDto: ResourceNotificationDto): ResourceResDto {
        val resource: ResourceDescription = resourceUpdateUseCase.updateResourceStatus(resourceId, ResourceRemoteUpdateCommand(ResourceStatusEnum.valueOf(resourceDto.status), resourceDto.resourceData))
        return ResourceResDto(
            id = resource.resourceId!!, 
            name = resource.resourceName, 
            resourceType = resource.resourceType.name, 
            resourceStatus = resource.resourceStatus!!.name,
            properties = toResourcePropertyDto(resource.resourceProperties)
        )
    }

    private fun toResourcePropertyDto(resourceProperties: List<ResourceProperty>): List<ResourcePropertyDto> {
        return resourceProperties.map {
            ResourcePropertyDto(it.propertyName, it.propertyValue)
        }
    }
}