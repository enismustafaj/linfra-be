package ai.linfra.resource_app.user.domain.api.impl

import ai.linfra.resource_app.user.domain.api.GetWaitListUserUseCase
import ai.linfra.resource_app.user.domain.model.WaitListUser
import ai.linfra.resource_app.user.persistence.WaitListUserEntity
import ai.linfra.resource_app.user.persistence.WaitListUserRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
@Transactional(readOnly = true)
class GetWaitListUserImpl(
    private val waitListUserRepository: WaitListUserRepository
) : GetWaitListUserUseCase {
    private fun toWaitListUserModel(waitListUser: WaitListUserEntity): WaitListUser {
        return WaitListUser(
            userId = waitListUser.id.toString(),
            email = waitListUser.email,
            name = waitListUser.name
        )
    }

    override fun get(userId: String): Optional<WaitListUser> {
        val waitListUser: Optional<WaitListUserEntity> =
            waitListUserRepository.findByUserId(userId)
        return waitListUser.map(this::toWaitListUserModel)
    }
}
