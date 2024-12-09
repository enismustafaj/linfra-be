package ai.linfra.resource_app.resource.domain.model.event

data class DatabaseResourceCreatedEvent(
    val resourceId: String,
    val databaseName: String,
    val databaseEngine: String,
    val databaseInstanceClass: String,
)
