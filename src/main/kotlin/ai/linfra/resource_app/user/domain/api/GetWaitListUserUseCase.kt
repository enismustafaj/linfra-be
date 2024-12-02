package ai.linfra.resource_app.user.domain.api

import ai.linfra.resource_app.user.domain.model.WaitListUser
import java.util.*

interface GetWaitListUserUseCase {
    fun get(userId: String): Optional<WaitListUser>
}
