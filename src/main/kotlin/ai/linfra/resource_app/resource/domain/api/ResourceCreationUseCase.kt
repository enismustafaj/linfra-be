package ai.linfra.resource_app.resource.domain.api

import ai.linfra.resource_app.resource.domain.model.ResourceDescription

interface ResourceCreationUseCase {
    fun createResource(chatId: String, resource: ResourceDescription): ResourceDescription
}