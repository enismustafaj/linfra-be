package ai.linfra.resource_app.user.domain.api.impl;

import org.springframework.stereotype.Component
import ai.linfra.resource_app.user.domain.api.GetUserUseCase
import ai.linfra.resource_app.user.domain.model.User
import ai.linfra.resource_app.user.persistence.UserEntity
import ai.linfra.resource_app.user.persistence.UserRepository
import java.util.*

@Component
class GetUserImpl(
    val userRepository: UserRepository
): GetUserUseCase {

    override fun getUserById(id: String): User {
        val user: UserEntity = userRepository.findById(UUID.fromString(id)).orElseThrow()
        return User(user.id.toString(), user.fullName, user.email)
    }

}