package ai.linfra.resource_app.resource.rest

import ai.linfra.resource_app.resource.rest.dto.ResourceSuggestionDto
import ai.linfra.resource_app.resource.rest.gateway.ResourceSuggestionGateway
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/resources/suggestions")
class ResourceSuggestionController(
        private val resourceSuggestionGateway: ResourceSuggestionGateway
) {

    @PutMapping("/threads/{threadId}")
    fun createResourceSuggestion(@PathVariable threadId: String): ResourceSuggestionDto {
        return resourceSuggestionGateway.getResourceSuggestion(threadId)
    }
}
