package ai.linfra.resource_app.resource.rest.gateway

import ai.linfra.resource_app.resource.domain.api.ResourceProposalUseCase
import ai.linfra.resource_app.resource.rest.dto.ResourceSuggestionDto
import org.springframework.stereotype.Component

@Component
class ResourceSuggestionGateway(private val resourceSuggestionUseCase: ResourceProposalUseCase) {

    fun getResourceSuggestion(threadId: String): ResourceSuggestionDto {
        return ResourceSuggestionDto(resourceSuggestionUseCase.getResourceSuggestions(threadId))
    }
}
