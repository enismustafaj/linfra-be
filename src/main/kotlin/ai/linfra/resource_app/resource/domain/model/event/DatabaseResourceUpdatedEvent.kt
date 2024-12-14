package ai.linfra.resource_app.resource.domain.model.event

data class DatabaseResourceUpdatedEvent(
    val resourceId: String,
    val resourceNewName: String,
    val resourceNewEngine: String,
    val resourceNewInstanceClass: String
)
