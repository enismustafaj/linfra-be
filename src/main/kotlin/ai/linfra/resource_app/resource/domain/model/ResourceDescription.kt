package ai.linfra.resource_app.resource.domain.model

data class ResourceDescription(
    val resourceName: String,
    val resourceProperties: List<ResourceProperty>
)
