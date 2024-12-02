package ai.linfra.resource_app.user.domain.api.impl

import ai.linfra.resource_app.user.domain.api.GetUserIdUseCase
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class GetUserIdUseCaseImpl : GetUserIdUseCase {

    override fun get(): String {
        return getUserIdClaim()
    }

    private fun getUserIdClaim(): String {
        // todo: find if there's a native way of achieving this?
        val principal: Jwt = SecurityContextHolder.getContext().authentication.principal as Jwt
        return principal.getClaimAsString("user_id")
    }
}
