package ai.linfra.resource_app.user.domain.api;

import ai.linfra.resource_app.user.domain.model.User

interface GetUserUseCase {

    fun getUserById(id: String): User;
}