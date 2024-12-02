package ai.linfra.resource_app.user.rest

import ai.linfra.resource_app.user.rest.dto.WaitListUserRequest
import ai.linfra.resource_app.user.rest.dto.WaitListUserResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/v1/user/waitlist")
class UserWaitListController(
    private val waitlistGateway: WaitlistGateway
) {

    @PostMapping
    fun addUserToWaitList(@RequestBody user: WaitListUserRequest): ResponseEntity<WaitListUserResponse> {
        val response: WaitListUserResponse = waitlistGateway.create(user)
        return ResponseEntity.created(URI("/api/v1/user/waitlist/" + response.id)).body(
            response
        )
    }

    @GetMapping
    fun getUserWaitlist(): ResponseEntity<WaitListUserResponse> {
        // todo: consider 404?
        val response: WaitListUserResponse? = waitlistGateway.get()
        return ResponseEntity.created(URI("/api/v1/user/waitlist/" + response?.id)).body(
            response
        )
    }
}
