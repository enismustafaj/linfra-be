package ai.linfra.resource_app.resource.domain.api.impl.strategies

import ai.linfra.resource_app.chat.domain.api.UpdateResourceChatUseCase
import ai.linfra.resource_app.chat.domain.command.UpdateChatMetadataCommand
import ai.linfra.resource_app.resource.domain.api.impl.ResourceCreationStrategy
import ai.linfra.resource_app.resource.domain.model.ResourceDescription
import ai.linfra.resource_app.resource.domain.model.ResourceProperty
import ai.linfra.resource_app.resource.domain.model.ResourceTypeEnum
import ai.linfra.resource_app.resource.domain.model.event.DatabaseResourceCreatedEvent
import ai.linfra.resource_app.resource.domain.model.ResourceStatusEnum
import ai.linfra.resource_app.resource.persistence.ResourceEntity
import ai.linfra.resource_app.resource.persistence.ResourceRepository
import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.util.*

@Component
class DatabaseResourceCreationStrategy(
    private val resourceRepository: ResourceRepository,
    private val updateResourceChatUseCase: UpdateResourceChatUseCase,
    private val applicationEventPublisher: ApplicationEventPublisher
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

        applicationEventPublisher.publishEvent(createDatabaseCreatedEvent(savedResource))
        return toResourceDomainModel(savedResource)
    }

    private fun createDatabaseCreatedEvent(savedResource: ResourceEntity ): DatabaseResourceCreatedEvent {
        return DatabaseResourceCreatedEvent(
            resourceId = savedResource.id.toString(),
            databaseName = savedResource.resourceName!!,
            databaseEngine = savedResource.resourceProperties!!.find { it.propertyName == "engine" }?.propertyValue ?: "mysql",
            databaseInstanceClass = savedResource.resourceProperties!!.find { it.propertyName == "instance_class" }?.propertyValue ?: "db.t2.micro"
        )
    }

    private fun toResourceEntity(resource: ResourceDescription): ResourceEntity {
        return ResourceEntity(
            resourceName = resource.resourceName,
            resourceType = ResourceTypeEnum.DATABASE,
            resourceStatus = ResourceStatusEnum.CREATING,
            userId = UUID.randomUUID().toString(),
            chatThreadId = resource.chatThreadId,
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

    private fun toResourceDomainModel(resource: ResourceEntity): ResourceDescription {
        return ResourceDescription(
            resourceId = resource.id!!.toString(),
            resourceName = resource.resourceName!!,
            resourceType = resource.resourceType!!,
            resourceStatus = resource.resourceStatus!!,
            resourceProperties = toResourcePropertiesDomainModel(resource.resourceProperties!!),
            chatThreadId = resource.chatThreadId!!
        )
    }

    private fun toResourcePropertiesDomainModel(resourceProperties: List<ai.linfra.resource_app.resource.persistence.ResourceProperty>): List<ResourceProperty> {
        return resourceProperties.map {
            ResourceProperty(
                propertyName = it.propertyName,
                propertyValue = it.propertyValue
            )
        }
    }
}