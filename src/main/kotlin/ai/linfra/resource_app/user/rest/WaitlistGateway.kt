package ai.linfra.resource_app.user.rest

import ai.linfra.resource_app.user.domain.api.CreateWaitListUserUseCase
import ai.linfra.resource_app.user.domain.api.GetUserIdUseCase
import ai.linfra.resource_app.user.domain.api.GetWaitListUserUseCase
import ai.linfra.resource_app.user.domain.model.WaitListUser
import ai.linfra.resource_app.user.rest.dto.WaitListUserRequest
import ai.linfra.resource_app.user.rest.dto.WaitListUserResponse
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Component
class WaitlistGateway(
    private val getWaitListUserUseCase: GetWaitListUserUseCase,
    private val createWaitListUserUseCase: CreateWaitListUserUseCase,
    private val getUserIdUseCase: GetUserIdUseCase
) {

    @Transactional
    fun create(user: WaitListUserRequest): WaitListUserResponse {
        val userId: String = getUserIdUseCase.get()
        val waitListUser = WaitListUser(userId = userId, name = user.name, email = user.email);
        val saved: WaitListUser = createWaitListUserUseCase.createWaitListUser(waitListUser)
        return WaitListUserResponse(
            id = saved.userId,
            name = saved.name,
            email = saved.email
        )
    }

    fun get(): WaitListUserResponse? {
        val userId: String = getUserIdUseCase.get()
        val waitListUser: Optional<WaitListUser> = getWaitListUserUseCase.get(userId)
        return waitListUser.map(this::mapToResponse).getOrNull()
    }

    private fun mapToResponse(user: WaitListUser): WaitListUserResponse {
        return WaitListUserResponse(
            id = user.userId,
            name = user.name,
            email = user.email
        )
    }
}
