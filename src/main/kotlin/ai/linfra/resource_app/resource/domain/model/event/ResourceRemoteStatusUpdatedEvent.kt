package ai.linfra.resource_app.resource.domain.model.event

import com.google.gson.JsonObject

data class ResourceRemoteStatusUpdatedEvent(
    val resourceId: String,
    val status: String,
    val metadata: JsonObject?
)