package ai.linfra.resource_app.user.domain.api.impl

import ai.linfra.resource_app.user.domain.api.CreateWaitListUserUseCase
import ai.linfra.resource_app.user.domain.model.WaitListUser
import ai.linfra.resource_app.user.persistence.WaitListUserEntity
import ai.linfra.resource_app.user.persistence.WaitListUserRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateWaitListUserImpl(
    private val waitListUserRepository: WaitListUserRepository
) : CreateWaitListUserUseCase {

    override fun createWaitListUser(waitListUser: WaitListUser): WaitListUser {
        val savedUser: WaitListUserEntity =
            waitListUserRepository.save(createWaitListUserEntity(waitListUser))
        return toWaitListUserModel(savedUser)
    }

    private fun toWaitListUserModel(waitListUser: WaitListUserEntity): WaitListUser {
        return WaitListUser(
            userId = waitListUser.userId,
            email = waitListUser.email,
            name = waitListUser.name
        )
    }

    private fun createWaitListUserEntity(waitListUser: WaitListUser): WaitListUserEntity {
        return WaitListUserEntity(
            userId = waitListUser.userId,
            email = waitListUser.email,
            name = waitListUser.name
        )
    }
}
