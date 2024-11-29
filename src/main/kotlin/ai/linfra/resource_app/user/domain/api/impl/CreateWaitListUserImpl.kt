package ai.linfra.resource_app.user.domain.api.impl

import ai.linfra.resource_app.user.domain.api.CreateWaitListUserUseCase
import ai.linfra.resource_app.user.domain.model.WaitListUser
import ai.linfra.resource_app.user.persistence.WaitListUserEntity
import ai.linfra.resource_app.user.persistence.WaitListUserRepository
import org.springframework.stereotype.Component

@Component
class CreateWaitListUserImpl(
    private val waitListUserRepository: WaitListUserRepository
): CreateWaitListUserUseCase {
    override fun createWaitListUser(waitListUser: WaitListUser): WaitListUser {
        val savedUser: WaitListUserEntity = waitListUserRepository.save(createWaitListUserEntity(waitListUser))
        return toWaitListUserModel(savedUser)
    }

    private fun toWaitListUserModel(waitListUser: WaitListUserEntity): WaitListUser {
        return WaitListUser(userId = waitListUser.id.toString(), email = waitListUser.email, fullName = waitListUser.fullName)
    }

    private fun createWaitListUserEntity(waitListUser: WaitListUser): WaitListUserEntity {
        return WaitListUserEntity(email = waitListUser.email, fullName = waitListUser.fullName)
    }
}