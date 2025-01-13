package ai.linfra.resource_app.notification.domain.model;

data class EmailPayload(
    val sender: String,
    val recipient: String,
    val template: String,
    val emailData: Map<String, Any>
)