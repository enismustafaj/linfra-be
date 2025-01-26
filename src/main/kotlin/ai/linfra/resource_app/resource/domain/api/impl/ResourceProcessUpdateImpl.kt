package ai.linfra.resource_app.resource.domain.api.impl

import org.springframework.stereotype.Component
import jakarta.transaction.Transactional
import java.util.UUID

import ai.linfra.resource_app.resource.domain.model.ResourceDescription
import ai.linfra.resource_app.resource.persistence.ResourceEntity
import ai.linfra.resource_app.resource.persistence.ResourceProperty
import ai.linfra.resource_app.resource.domain.api.ResourceRemoteUpdateUseCase
import ai.linfra.resource_app.resource.domain.model.ResourceStatusEnum
import ai.linfra.resource_app.resource.domain.model.command.ResourceRemoteUpdateCommand
import ai.linfra.resource_app.resource.persistence.ResourceRepository
import ai.linfra.resource_app.resource.security.domain.api.EncryptResourceSecretsUseCase

@Component
class ResourceProcessUpdateImpl(
    private val resourceRepository: ResourceRepository,
    private val encryptResourceSecretsUseCase: EncryptResourceSecretsUseCase
): ResourceRemoteUpdateUseCase {

    @Transactional
    override fun updateResourceStatus(resourceId: String, command: ResourceRemoteUpdateCommand): ResourceDescription {
        val resourceEntity: ResourceEntity = resourceRepository.findById(UUID.fromString(resourceId)).orElseThrow { IllegalArgumentException("Resource with id $resourceId not found") }
        resourceEntity.resourceStatus = ResourceStatusEnum.valueOf(command.status.name)

        if (command.status == ResourceStatusEnum.CREATED) {
            encryptResourceSecretsUseCase.encryptSecrets(command.data, resourceId)
        }

        return toResourceDescription(resourceEntity)
    }

    private fun toResourceDescription(resourceEntity: ResourceEntity): ResourceDescription {
        return ResourceDescription(
            resourceId = resourceEntity.id.toString(),
            resourceName = resourceEntity.resourceName!!,
            resourceType = resourceEntity.resourceType!!,
            resourceStatus = resourceEntity.resourceStatus!!,
            resourceProperties = resourceEntity.resourceProperties!!.map { toResourceProperty(it) },
            chatThreadId = resourceEntity.chatThreadId!!
        )
    }

    private fun toResourceProperty(resourceProperty: ResourceProperty): ai.linfra.resource_app.resource.domain.model.ResourceProperty {
        return ai.linfra.resource_app.resource.domain.model.ResourceProperty(
            propertyName = resourceProperty.propertyName,
            propertyValue = resourceProperty.propertyValue
        )
    }

}