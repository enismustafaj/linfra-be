package ai.linfra.resource_app.resource.domain.api.impl

import ai.linfra.resource_app.resource.domain.model.ResourceDescription

interface ResourceUpdateStrategy {
    fun supportResourceType(resourceType: String): Boolean

    fun updateResource(chatId: String, resourceId: String, resource: ResourceDescription): ResourceDescription
}