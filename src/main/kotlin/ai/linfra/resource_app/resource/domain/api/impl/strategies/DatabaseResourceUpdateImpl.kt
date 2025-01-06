package ai.linfra.resource_app.resource.domain.api.impl.strategies

import ai.linfra.resource_app.chat.domain.api.UpdateResourceChatUseCase
import ai.linfra.resource_app.chat.domain.command.UpdateChatMetadataCommand
import ai.linfra.resource_app.resource.domain.api.impl.ResourceUpdateStrategy
import ai.linfra.resource_app.resource.domain.model.ResourceDescription
import ai.linfra.resource_app.resource.domain.model.ResourceTypeEnum
import ai.linfra.resource_app.resource.domain.model.ResourceStatusEnum
import ai.linfra.resource_app.resource.persistence.ResourceEntity
import ai.linfra.resource_app.resource.persistence.ResourceProperty
import ai.linfra.resource_app.resource.persistence.ResourceRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import java.util.*

@Component
class DatabaseResourceUpdateImpl(
    private val resourceRepository: ResourceRepository,
    private val updateResourceChatUseCase: UpdateResourceChatUseCase
): ResourceUpdateStrategy {

    override fun supportResourceType(resourceType: String): Boolean {
        return resourceType == ResourceTypeEnum.DATABASE.name
    }

    @Transactional
    override fun updateResource(
        chatId: String,
        resourceId: String,
        resource: ResourceDescription
    ): ResourceDescription {
        val resourceEntity: ResourceEntity = resourceRepository.findById(UUID.fromString(resourceId)).orElseThrow { IllegalArgumentException("Resource with id $resourceId not found") }
        resourceEntity.resourceProperties = resource.resourceProperties.map { toResourcePropertyEntity(it) }
        val savedResource: ResourceEntity = resourceRepository.save(resourceEntity)
        updateResourceChatUseCase.updateResourceChat(updateChatCommand(chatId, resourceId))

        return toResourceDescription(savedResource)
    }

    private fun toResourceDescription(resourceEntity: ResourceEntity): ResourceDescription {
        return ResourceDescription(
            resourceId = resourceEntity.id.toString(),
            resourceName = resourceEntity.resourceName!!,
            resourceType = resourceEntity.resourceType!!,
            resourceProperties = resourceEntity.resourceProperties!!.map { toResourceProperty(it) }
        )
    }

    private fun toResourceProperty(resourceProperty: ResourceProperty): ai.linfra.resource_app.resource.domain.model.ResourceProperty {
        return ai.linfra.resource_app.resource.domain.model.ResourceProperty(
            propertyName = resourceProperty.propertyName,
            propertyValue = resourceProperty.propertyValue
        )
    }

    private fun toResourcePropertyEntity(resourceProperty: ai.linfra.resource_app.resource.domain.model.ResourceProperty): ResourceProperty {
        return ResourceProperty(
            propertyName = resourceProperty.propertyName,
            propertyValue = resourceProperty.propertyValue
        )
    }

    private fun updateChatCommand(
        chatId: String,
        resourceId: String,
    ): UpdateChatMetadataCommand {
        return UpdateChatMetadataCommand(
            chatId = chatId,
            action = "update",
            reference = resourceId
        )
    }
}