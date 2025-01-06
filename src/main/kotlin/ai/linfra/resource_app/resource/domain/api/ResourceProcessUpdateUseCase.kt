package ai.linfra.resource_app.resource.domain.api

import ai.linfra.resource_app.resource.domain.model.ResourceDescription

interface ResourceProcessUpdateUseCase {
    fun updateResourceStatus(resourceId: String, status: String): ResourceDescription
}