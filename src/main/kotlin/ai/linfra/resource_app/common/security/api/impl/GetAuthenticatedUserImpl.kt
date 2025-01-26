package ai.linfra.resource_app.common.security.api.impl

import ai.linfra.resource_app.common.security.api.GetAuthenticatedUserUseCase
import ai.linfra.resource_app.common.security.model.AuthenticatedUser
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class GetAuthenticatedUserImpl(): GetAuthenticatedUserUseCase {

    override fun getAuthenticatedUserUseCase(): AuthenticatedUser {
        val principal: Jwt = SecurityContextHolder.getContext().authentication.principal as Jwt
        return AuthenticatedUser(email = principal.getClaimAsString("email"), userId = principal.getClaimAsString("user_id"))
    }
}