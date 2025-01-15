package ai.linfra.resource_app.resource.domain.model.event

data class ResourceRemoteStatusUpdatedEvent(
    val resourceId: String,
    val status: String,
    val metadata: Map<String, Any>?
)