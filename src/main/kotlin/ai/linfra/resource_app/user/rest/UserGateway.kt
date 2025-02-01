package ai.linfra.resource_app.user.rest

import ai.linfra.resource_app.user.domain.api.CreateUserUseCase
import ai.linfra.resource_app.user.domain.model.User
import ai.linfra.resource_app.user.domain.model.UserCreateCommand
import ai.linfra.resource_app.user.rest.dto.UserReqDto
import ai.linfra.resource_app.user.rest.dto.UserResDto
import org.springframework.stereotype.Component

@Component
class UserGateway(
    val createUserUseCase: CreateUserUseCase
) {

    fun createUser(userReq: UserReqDto): UserResDto {
        val createdUser: User = createUserUseCase.createUser(toUserCommand(userReq))

        return toUserResDto(createdUser)
    }

    private fun toUserCommand(userReq: UserReqDto): UserCreateCommand {
        return UserCreateCommand(
            fullName = userReq.fullName,
            email = userReq.email,
            password = userReq.password
        )
    }

    private fun toUserResDto(user: User): UserResDto {
        return UserResDto(
            id = user.id,
            fullName = user.fullName,
            email = user.email
        )
    }
}