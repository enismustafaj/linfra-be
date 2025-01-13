package ai.linfra.resource_app.user.domain.api.impl;

import org.springframework.stereotype.Component
import ai.linfra.resource_app.user.domain.api.GetUserUseCase
import ai.linfra.resource_app.user.domain.model.User

@Component
class GetUserImpl(): GetUserUseCase {

    override fun getUserById(id: String): User {
        return User("","")
    }

}