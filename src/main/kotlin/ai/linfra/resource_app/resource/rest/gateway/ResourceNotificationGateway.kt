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
import ai.linfra.resource_app.resource.security.domain.model.ResourceCredentials

@Component
class ResourceNotificationGateway(
    val resourceUpdateUseCase: ResourceRemoteUpdateUseCase
) {

    fun updateResourceStatus(resourceId: String, resourceDto: ResourceNotificationDto): ResourceResDto {
        val resource: ResourceDescription = resourceUpdateUseCase.updateResourceStatus(resourceId, toResourceRemoteUpdateCommand(resourceDto))
        return ResourceResDto(
            id = resource.resourceId!!, 
            name = resource.resourceName, 
            resourceType = resource.resourceType.name, 
            resourceStatus = resource.resourceStatus!!.name,
            properties = toResourcePropertyDto(resource.resourceProperties),
            chatThreadId = resource.chatThreadId
        )
    }

    private fun toResourceRemoteUpdateCommand(resourceDto: ResourceNotificationDto): ResourceRemoteUpdateCommand {
        val resourceCredentials = if (resourceDto.resourceCredentialsDto != null) ResourceCredentials(resourceDto.resourceCredentialsDto.username, resourceDto.resourceCredentialsDto.password) else null
        return ResourceRemoteUpdateCommand(ResourceStatusEnum.valueOf(resourceDto.status), resourceDto.url, resourceCredentials)
    }

    private fun toResourcePropertyDto(resourceProperties: List<ResourceProperty>): List<ResourcePropertyDto> {
        return resourceProperties.map {
            ResourcePropertyDto(it.propertyName, it.propertyValue)
        }
    }
}