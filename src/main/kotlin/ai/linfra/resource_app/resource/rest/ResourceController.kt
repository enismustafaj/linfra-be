package ai.linfra.resource_app.resource.rest

import ai.linfra.resource_app.resource.rest.dto.ResourceReqDto
import ai.linfra.resource_app.resource.rest.dto.ResourceResDto
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/resources")
class ResourceController(
    private val resourceGateway: ResourceGateway
) {

    @GetMapping
    fun getResource(): String {
        return "Resource"
    }

    @PostMapping
    fun createResource(@RequestBody resource: ResourceReqDto): ResourceResDto? {
        return resourceGateway.createResource(resource)
    }

    @PutMapping("/{resourceId}")
    fun updateResource(@PathVariable resourceId: String): String {
        return "Resource updated"
    }

    @DeleteMapping("/{resourceId}")
    fun deleteResource(@PathVariable resourceId: String): String {
        return "Resource deleted"
    }

}