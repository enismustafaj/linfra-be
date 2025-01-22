package ai.linfra.resource_app.common.security.model

data class AuthenticatedUser(
    val userId: String,
    val email: String
)
