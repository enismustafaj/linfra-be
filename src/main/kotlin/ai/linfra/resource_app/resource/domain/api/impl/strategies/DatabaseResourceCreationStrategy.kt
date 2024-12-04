package ai.linfra.resource_app.resource.domain.api.impl.strategies

import ai.linfra.resource_app.resource.domain.api.impl.ResourceCreationStrategy
import ai.linfra.resource_app.resource.domain.model.ResourceDescription
import ai.linfra.resource_app.resource.domain.model.ResourceTypeEnum
import org.springframework.stereotype.Component

@Component
class DatabaseResourceCreationStrategy: ResourceCreationStrategy {

    override fun supportResourceType(resourceType: String): Boolean {
        return resourceType == ResourceTypeEnum.DATABASE.name
    }

    override fun createResource(resource: ResourceDescription): ResourceDescription {



        return resource
    }
}