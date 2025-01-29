package ai.linfra.resource_app.user.domain.api.impl

import ai.linfra.resource_app.user.domain.api.CreateUserUseCase
import ai.linfra.resource_app.user.domain.model.User
import ai.linfra.resource_app.user.domain.model.UserCreateCommand
import ai.linfra.resource_app.user.persistence.UserEntity
import ai.linfra.resource_app.user.persistence.UserRepository
import org.springframework.stereotype.Component

@Component
class CreateUserImpl(
    val userRepository: UserRepository
): CreateUserUseCase {

    override fun createUser(user: UserCreateCommand): User {
        val savedUser: UserEntity = userRepository.save(toUserEntity(user))

        return User(
            id = savedUser.id.toString(),
            email = savedUser.email,
            fullName = savedUser.fullName
        )
    }

    private fun toUserEntity(user: UserCreateCommand): UserEntity {
        return UserEntity(
            fullName = user.fullName,
            email = user.email,
        )
    }

}