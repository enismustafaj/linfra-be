package ai.linfra.resource_app.user.domain.api.impl

import ai.linfra.resource_app.user.domain.api.CreateUserUseCase
import ai.linfra.resource_app.user.domain.dependencies.CreateFirebaseUser
import ai.linfra.resource_app.user.domain.model.User
import ai.linfra.resource_app.user.domain.model.UserCreateCommand
import ai.linfra.resource_app.user.domain.model.event.UserCreatedEvent
import ai.linfra.resource_app.user.persistence.UserEntity
import ai.linfra.resource_app.user.persistence.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class CreateUserImpl(
    val userRepository: UserRepository,
    val createFirebaseUser: CreateFirebaseUser,
    val applicationEventPublisher: ApplicationEventPublisher
): CreateUserUseCase {

    override fun createUser(user: UserCreateCommand): User {
        val firebaseUserId: String = createFirebaseUser.createFirebaseUser(user)
        val savedUser: UserEntity = userRepository.save(toUserEntity(user, firebaseUserId))

        applicationEventPublisher.publishEvent(UserCreatedEvent(savedUser.id.toString()))

        return User(
            id = savedUser.id.toString(),
            email = savedUser.email,
            fullName = savedUser.fullName,
        )
    }

    private fun toUserEntity(user: UserCreateCommand, referenceId: String): UserEntity {
        return UserEntity(
            fullName = user.fullName,
            email = user.email,
            referenceId = referenceId
        )
    }

}