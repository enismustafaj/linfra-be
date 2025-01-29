package ai.linfra.resource_app.user.domain.api

import ai.linfra.resource_app.user.domain.model.User
import ai.linfra.resource_app.user.domain.model.UserCreateCommand

interface CreateUserUseCase {
    fun createUser(user: UserCreateCommand): User
}