package ai.linfra.resource_app.resource.rest.dto

data class ResourceNotificationDto(
    val status: String,
    val url: String? = null,
    val resourceCredentialsDto: ResourceCredentialsDto? = null
)