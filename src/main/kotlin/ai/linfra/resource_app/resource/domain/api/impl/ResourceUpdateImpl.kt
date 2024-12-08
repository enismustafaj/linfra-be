package ai.linfra.resource_app.resource.domain.api.impl

import ai.linfra.resource_app.resource.domain.api.ResourceUpdateUseCase
import ai.linfra.resource_app.resource.domain.exception.ResourceTypeNotSupportedException
import ai.linfra.resource_app.resource.domain.model.ResourceDescription
import org.springframework.stereotype.Component

@Component
class ResourceUpdateImpl(
    private val resourceUpdateStrategies: List<ResourceUpdateStrategy>
): ResourceUpdateUseCase {
    override fun updateResource(
        chatId: String,
        resourceId: String,
        resource: ResourceDescription
    ): ResourceDescription {
        val strategy = resourceUpdateStrategies.find { it.supportResourceType(resource.resourceType.name) }
        return strategy?.updateResource(chatId, resourceId, resource) ?: throw ResourceTypeNotSupportedException("Resource type ${resource.resourceType.name} is not supported")
    }
}