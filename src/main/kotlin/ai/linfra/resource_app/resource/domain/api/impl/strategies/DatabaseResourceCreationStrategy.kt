package ai.linfra.resource_app.resource.domain.api.impl.strategies

import ai.linfra.resource_app.chat.domain.api.UpdateResourceChatUseCase
import ai.linfra.resource_app.chat.domain.command.UpdateChatMetadataCommand
import ai.linfra.resource_app.resource.domain.api.impl.ResourceCreationStrategy
import ai.linfra.resource_app.resource.domain.model.ResourceDescription
import ai.linfra.resource_app.resource.domain.model.ResourceProperty
import ai.linfra.resource_app.resource.domain.model.ResourceTypeEnum
import ai.linfra.resource_app.resource.domain.model.templates.DatabaseTemplate
import ai.linfra.resource_app.resource.persistence.ResourceEntity
import ai.linfra.resource_app.resource.persistence.ResourceRepository
import gg.jte.TemplateEngine
import gg.jte.output.StringOutput
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import java.util.*


@Component
class DatabaseResourceCreationStrategy(
    private val resourceRepository: ResourceRepository,
    private val updateResourceChatUseCase: UpdateResourceChatUseCase,
    private val templateEngine: TemplateEngine
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

        println(createResourceTemplate(
            DatabaseTemplate(
                name = resource.resourceName,
                engine = resource.resourceProperties.find { it.propertyName == "engine" }?.propertyValue ?: "mysql",
                instanceClass = resource.resourceProperties.find { it.propertyName == "instance_class" }?.propertyValue ?: "db.t2.micro"
            )
        ))
        return toResourceDomainModel(savedResource)
    }

    private fun createResourceTemplate(resource: DatabaseTemplate): String {
        val output = StringOutput()
        templateEngine.render("database.kte", mapOf("database" to resource), output)

        return output.toString()
    }

    private fun toResourceEntity(resource: ResourceDescription): ResourceEntity {
        return ResourceEntity(
            resourceName = resource.resourceName,
            resourceType = ResourceTypeEnum.DATABASE,
            userId = UUID.randomUUID(),
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
            resourceProperties = toResourcePropertiesDomainModel(resource.resourceProperties!!)
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