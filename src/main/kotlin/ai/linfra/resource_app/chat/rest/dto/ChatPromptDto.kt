package ai.linfra.resource_app.chat.rest.dto

import ai.linfra.resource_app.chat.domain.model.ChatRoleEnum

data class ChatPromptDto(
    val role: ChatRoleEnum,
    val text: String,
    val metadata: ChatMetadataDto? = null
)