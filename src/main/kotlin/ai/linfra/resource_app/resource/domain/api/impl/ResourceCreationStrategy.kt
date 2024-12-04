package ai.linfra.resource_app.resource.domain.api.impl

import ai.linfra.resource_app.resource.domain.model.ResourceDescription

interface ResourceCreationStrategy {

    fun supportResourceType(resourceType: String): Boolean

    fun createResource(resource: ResourceDescription): ResourceDescription
}