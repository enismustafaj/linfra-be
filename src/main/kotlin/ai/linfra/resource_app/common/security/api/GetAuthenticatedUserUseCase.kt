package ai.linfra.resource_app.common.security.api

import ai.linfra.resource_app.common.security.model.AuthenticatedUser

interface GetAuthenticatedUserUseCase {

    fun getAuthenticatedUserUseCase(): AuthenticatedUser
}