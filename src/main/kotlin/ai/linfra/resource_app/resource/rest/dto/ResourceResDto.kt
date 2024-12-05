package ai.linfra.resource_app.resource.rest.dto

data class ResourceResDto(
    val id: String,
    val name: String,
    val resourceType: String,
    val properties: List<ResourcePropertyDto>
)
