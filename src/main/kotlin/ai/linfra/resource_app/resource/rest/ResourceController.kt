package ai.linfra.resource_app.resource.rest

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/resource")
class ResourceController {

    @GetMapping
    fun getResource(): String {
        return "Resource"
    }

    @PostMapping
    fun createResource(): String {
        return "Resource created"
    }

    @PutMapping("/{resourceId}")
    fun updateResource(@PathVariable resourceId: String): String {
        return "Resource updated"
    }

}