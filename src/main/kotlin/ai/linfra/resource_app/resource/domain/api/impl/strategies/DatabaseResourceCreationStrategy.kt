package ai.linfra.resource_app.resource.domain.api.impl.strategies

import ai.linfra.resource_app.chat.domain.api.UpdateResourceChatUseCase
import ai.linfra.resource_app.chat.domain.command.UpdateChatMetadataCommand
import ai.linfra.resource_app.resource.domain.api.impl.ResourceCreationStrategy
import ai.linfra.resource_app.resource.domain.model.ResourceDescription
import ai.linfra.resource_app.resource.domain.model.ResourceProperty
import ai.linfra.resource_app.resource.domain.model.ResourceTypeEnum
import ai.linfra.resource_app.resource.persistence.ResourceEntity
import ai.linfra.resource_app.resource.persistence.ResourceRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

@Component
class DatabaseResourceCreationStrategy(
    private val resourceRepository: ResourceRepository,
    private val updateResourceChatUseCase: UpdateResourceChatUseCase
): ResourceCreationStrategy {

    override fun supportResourceType(resourceType: String): Boolean {
        return resourceType == ResourceTypeEnum.DATABASE.name
    }

    @Transactional
    override fun createResource(chatId: String, resource: ResourceDescription): ResourceDescription {

        val resourceEntity = toResourceEntity(resource)
        val savedResource = resourceRepository.save(resourceEntity)

        updateResourceChatUseCase.updateResourceChat(UpdateChatMetadataCommand(
            chatId = chatId,
            action = "resource_created",
            reference = savedResource.id.toString()
        ))

        return resource
    }

    private fun toResourceEntity(resource: ResourceDescription): ResourceEntity {
        return ResourceEntity(
            resourceName = resource.resourceName,
            resourceType = ResourceTypeEnum.DATABASE,
            resourceProperties = toResourceProperties(resource.resourceProperties)
        )
    }

    private fun toResourceProperties(resourceProperties: List<ResourceProperty>): List<ai.linfra.resource_app.resource.persistence.ResourceProperty> {
        return resourceProperties.map {
            ai.linfra.resource_app.resource.persistence.ResourceProperty(
                propertyName = it.propertyName,
                propertyValue = it.propertyValue
            )
        }
    }
}