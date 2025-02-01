package ai.linfra.resource_app.user.rest

import ai.linfra.resource_app.user.rest.dto.UserReqDto
import ai.linfra.resource_app.user.rest.dto.UserResDto
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/users")
class UserController(
    val userGateway: UserGateway
) {

    @PostMapping
    fun createUser(@RequestBody userReqDto: UserReqDto): UserResDto {
        return userGateway.createUser(userReqDto)
    }
}