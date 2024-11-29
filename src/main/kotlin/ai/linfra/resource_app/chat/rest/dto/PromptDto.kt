package ai.linfra.resource_app.chat.rest.dto

import ai.linfra.resource_app.chat.domain.model.ChatRoleEnum

data class PromptDto(
    val role: ChatRoleEnum,
    val text: String
)