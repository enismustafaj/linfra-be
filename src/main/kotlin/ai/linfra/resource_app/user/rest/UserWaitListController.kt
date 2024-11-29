package ai.linfra.resource_app.user.rest

import ai.linfra.resource_app.user.domain.api.CreateWaitListUserUseCase
import ai.linfra.resource_app.user.domain.model.WaitListUser
import ai.linfra.resource_app.user.rest.dto.UserWaitListReqDTO
import ai.linfra.resource_app.user.rest.dto.UserWaitListResDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/v1/user/waitlist")
class UserWaitListController(
    private val createWaitListUserUseCase: CreateWaitListUserUseCase
) {

    @PostMapping
    fun addUserToWaitList(@RequestBody user: UserWaitListReqDTO): ResponseEntity<UserWaitListResDTO> {
        val waitListUser: WaitListUser = createWaitListUserUseCase.createWaitListUser(WaitListUser(fullName = user.fullName, email = user.email))
        return ResponseEntity.created(URI("/api/v1/user/waitlist/" + waitListUser.userId)).body(
            UserWaitListResDTO(
                id = waitListUser.userId!!,
                fullName = waitListUser.fullName,
                email = waitListUser.email
            )
        )
    }
}