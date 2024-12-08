package ai.linfra.resource_app.chat.domain.api

import ai.linfra.resource_app.chat.domain.command.UpdateChatMetadataCommand

interface UpdateResourceChatUseCase {
    fun updateResourceChat(updateChatMetadataCommand: UpdateChatMetadataCommand)
}