package ai.linfra.resource_app.resource.domain.api

import ai.linfra.resource_app.resource.domain.model.ResourceDescription

interface ResourceProposalUseCase {
    fun getResourceSuggestions(contentDescription: String): ResourceDescription
}