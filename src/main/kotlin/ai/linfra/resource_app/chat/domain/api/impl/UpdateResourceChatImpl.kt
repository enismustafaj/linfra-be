package ai.linfra.resource_app.chat.domain.api.impl;

import ai.linfra.resource_app.chat.domain.api.UpdateResourceChatUseCase
import ai.linfra.resource_app.chat.domain.command.UpdateChatMetadataCommand
import ai.linfra.resource_app.chat.domain.exception.ChatUpdateNotAllowedException
import ai.linfra.resource_app.chat.domain.model.ChatRoleEnum
import ai.linfra.resource_app.chat.persistence.ChatMetadata
import ai.linfra.resource_app.chat.persistence.ChatThreadRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class UpdateResourceChatImpl(
    private val chatThreadRepository: ChatThreadRepository
): UpdateResourceChatUseCase {

    override fun updateResourceChat(updateChatMetadataCommand: UpdateChatMetadataCommand) {
        val chatThread = chatThreadRepository.findById(UUID.fromString(updateChatMetadataCommand.chatId)).get()

        if (chatThread.role != ChatRoleEnum.resource_manager) {
            throw ChatUpdateNotAllowedException("Chat update not allowed for this role")
        }

        chatThread.chatMetadata = toMetadata(updateChatMetadataCommand.action, updateChatMetadataCommand.reference)
        chatThreadRepository.save(chatThread)
    }

    private fun toMetadata(action: String, reference: String): ChatMetadata {
        return ChatMetadata(action, reference)
    }

}
