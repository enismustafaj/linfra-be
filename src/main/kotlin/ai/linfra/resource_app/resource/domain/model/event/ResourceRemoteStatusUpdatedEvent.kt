package ai.linfra.resource_app.resource.domain.model.event

data class ResourceRemoteStatusUpdatedEvent(
    val resourceId: String,
    val userId: String,
    val status: String,
    val metadata: Map<String, Any>?
)