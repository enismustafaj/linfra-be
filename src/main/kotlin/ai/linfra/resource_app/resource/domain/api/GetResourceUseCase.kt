package ai.linfra.resource_app.resource.domain.api

import ai.linfra.resource_app.resource.domain.model.ResourceDescription

interface GetResourceUseCase {
    fun getResource(resourceId: String): ResourceDescription
}