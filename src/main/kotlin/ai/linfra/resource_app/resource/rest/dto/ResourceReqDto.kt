package ai.linfra.resource_app.resource.rest.dto

data class ResourceReqDto(
    val chatId: String,
    val chatThreadId: String,
    val name: String,
    val resourceType: String,
    val properties: List<ResourcePropertyDto>
)
