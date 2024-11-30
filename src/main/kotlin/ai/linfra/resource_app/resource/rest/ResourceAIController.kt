package ai.linfra.resource_app.resource.rest

import ai.linfra.resource_app.resource.domain.api.ResourceProposalUseCase
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/ai/resource")
class ResourceAIController(
    private val resourceProposalUseCase: ResourceProposalUseCase
) {

    @PostMapping
    fun getResourceSuggestion() {

    }

}