package ai.linfra.resource_app.user.domain.api

import ai.linfra.resource_app.user.domain.model.WaitListUser

interface CreateWaitListUserUseCase {
    fun createWaitListUser(waitListUser: WaitListUser): WaitListUser
}