package ai.linfra.resource_app.resource.domain.model

data class ResourceDescription(
    val resourceName: String,
    val resourceType: ResourceTypeEnum,
    val resourceProperties: List<ResourceProperty>
)
