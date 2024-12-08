package ai.linfra.resource_app.resource.domain.api

import ai.linfra.resource_app.resource.domain.model.ResourceDescription

interface ResourceUpdateUseCase {
    fun updateResource(chatId: String, resourceId: String, resource: ResourceDescription): ResourceDescription
}