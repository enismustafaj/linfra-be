package ai.linfra.resource_app.resource.domain.api.impl

import ai.linfra.resource_app.chat.domain.api.UpdateResourceChatUseCase
import ai.linfra.resource_app.chat.domain.command.UpdateChatMetadataCommand
import ai.linfra.resource_app.resource.domain.api.DeleteResourceUseCase
import ai.linfra.resource_app.resource.domain.model.event.DatabaseResourceDeletedEvent
import ai.linfra.resource_app.resource.domain.model.ResourceStatusEnum
import ai.linfra.resource_app.resource.persistence.ResourceRepository
import ai.linfra.resource_app.resource.persistence.ResourceEntity
import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.util.*

@Component
class DeleteResourceImpl(
    private val resourceRepository: ResourceRepository,
    private val resourceChatUseCase: UpdateResourceChatUseCase,
    private val applicationEventPublisher: ApplicationEventPublisher
): DeleteResourceUseCase {

    @Transactional
    override fun deleteResource(chatId: String, resourceId: String) {
        val resourceEntity: ResourceEntity = resourceRepository.findById(UUID.fromString(resourceId)).orElseThrow { IllegalArgumentException("Resource with id $resourceId not found") }
        resourceEntity.resourceStatus = ResourceStatusEnum.DELETING
        resourceChatUseCase.updateResourceChat(updateChatCommand(chatId, resourceId))
        applicationEventPublisher.publishEvent(DatabaseResourceDeletedEvent(resourceId))
    }

    private fun updateChatCommand(chatId: String, resourceId: String): UpdateChatMetadataCommand {
        return UpdateChatMetadataCommand(
            chatId = chatId,
            action = "delete",
            reference = resourceId
        )
    }
}