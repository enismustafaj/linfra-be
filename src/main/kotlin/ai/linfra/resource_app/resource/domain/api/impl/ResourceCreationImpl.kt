package ai.linfra.resource_app.resource.domain.api.impl

import ai.linfra.resource_app.resource.domain.api.ResourceCreationUseCase
import ai.linfra.resource_app.resource.domain.model.ResourceDescription
import org.springframework.stereotype.Component

@Component
class ResourceCreationImpl(
    private val resourceCreationStrategies: List<ResourceCreationStrategy>
): ResourceCreationUseCase {

    override fun createResource(chatId: String, resource: ResourceDescription): ResourceDescription {
        val strategy = resourceCreationStrategies.find { it.supportResourceType(resource.resourceType.name) }
        return strategy?.createResource(chatId, resource) ?: resource
    }
}