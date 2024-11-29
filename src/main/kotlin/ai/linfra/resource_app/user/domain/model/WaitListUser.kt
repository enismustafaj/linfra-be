package ai.linfra.resource_app.user.domain.model

data class WaitListUser(
    val userId: String? = null,
    val fullName: String,
    val email: String,
)
