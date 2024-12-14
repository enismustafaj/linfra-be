package ai.linfra.resource_app.resource.rest

import ai.linfra.resource_app.resource.rest.dto.ResourceReqDto
import ai.linfra.resource_app.resource.rest.dto.ResourceResDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/resources")
class ResourceController(
    private val resourceGateway: ResourceGateway
) {

    @GetMapping("/{resourceId}")
    fun getResource(@PathVariable resourceId: String): ResourceResDto {
        return resourceGateway.getResource(resourceId)
    }

    @PostMapping
    fun createResource(@RequestBody resource: ResourceReqDto): ResourceResDto {
        return resourceGateway.createResource(resource)
    }

    @PutMapping("/{resourceId}")
    fun updateResource(@PathVariable resourceId: String, @RequestBody resourceUpdatePayload: ResourceReqDto): ResourceResDto {
        return resourceGateway.updateResource(resourceId, resourceUpdatePayload)
    }

    @DeleteMapping("/{resourceId}")
    fun deleteResource(@PathVariable resourceId: String, @RequestParam(required = true) chatId: String): ResponseEntity<Void> {
        resourceGateway.deleteResource(chatId, resourceId)

        return ResponseEntity.noContent().build()
    }

}