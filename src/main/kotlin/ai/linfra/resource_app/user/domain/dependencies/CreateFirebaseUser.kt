package ai.linfra.resource_app.user.domain.dependencies

import ai.linfra.resource_app.user.domain.model.UserCreateCommand

interface CreateFirebaseUser {
    fun createFirebaseUser(user: UserCreateCommand): String
}