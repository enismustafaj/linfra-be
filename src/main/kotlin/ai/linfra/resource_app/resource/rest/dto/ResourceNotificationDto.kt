package ai.linfra.resource_app.resource.rest.dto

import com.google.gson.JsonObject

data class ResourceNotificationDto(
    val status: String,
    val resourceData: JsonObject
)