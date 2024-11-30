package ai.linfra.resource_app.resource.domain.api.impl

import ai.linfra.resource_app.resource.domain.api.ResourceProposalUseCase
import ai.linfra.resource_app.resource.domain.dependencies.AiResourceSuggestionModel
import ai.linfra.resource_app.resource.domain.model.ResourceDescription
import org.springframework.stereotype.Component

@Component
class ResourceProposalImpl(
    private val aiResourceSuggestionModel: AiResourceSuggestionModel
): ResourceProposalUseCase {
    override fun getResourceSuggestions(contentDescription: String): ResourceDescription {
        TODO("Not yet implemented")
    }
}