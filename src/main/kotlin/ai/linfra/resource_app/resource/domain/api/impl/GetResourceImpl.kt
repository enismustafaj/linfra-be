package ai.linfra.resource_app.resource.domain.api.impl

import ai.linfra.resource_app.resource.domain.api.GetResourceUseCase
import ai.linfra.resource_app.resource.domain.model.ResourceDescription
import ai.linfra.resource_app.resource.domain.model.ResourceProperty
import ai.linfra.resource_app.resource.persistence.ResourceRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class GetResourceImpl(
    private val resourceRepository: ResourceRepository
): GetResourceUseCase {

    override fun getResource(resourceId: String): ResourceDescription {
        val resource = resourceRepository.findById(UUID.fromString(resourceId)).orElseThrow { IllegalArgumentException("Resource not found") }
        return ResourceDescription(
            resourceId = resource.id.toString(),
            resourceName = resource.resourceName!!,
            resourceType = resource.resourceType!!,
            resourceProperties = toResourcePropertyDomain(resource.resourceProperties!!)
        )
    }

    private fun toResourcePropertyDomain(resourceProperties: List<ai.linfra.resource_app.resource.persistence.ResourceProperty>): List<ResourceProperty> {
        return resourceProperties.map {
            ResourceProperty(it.propertyName, it.propertyValue)
        }
    }
}