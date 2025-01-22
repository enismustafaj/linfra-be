package ai.linfra.resource_app.resource.rest.gateway

import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.http.ResponseEntity

import ai.linfra.resource_app.resource.rest.dto.ResourceResDto;
import ai.linfra.resource_app.resource.rest.dto.ResourceNotificationDto

@RestController
@RequestMapping("/api/v1/notifications/resources")
class ResourceNotificationController(
    val resourceNotificationGateway: ResourceNotificationGateway
) {

    @PutMapping("/{resourceId}")
    fun updateResource(@PathVariable requestId: String, @RequestBody resourceNotification: ResourceNotificationDto): ResponseEntity<ResourceResDto> {

        return ResponseEntity.ok(resourceNotificationGateway.updateResourceStatus(requestId, resourceNotification))
    }
}