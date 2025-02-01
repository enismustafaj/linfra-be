package ai.linfra.resource_app.user.domain.model

data class UserCreateCommand(
    val fullName: String,
    val email: String,
    val password: String
)
