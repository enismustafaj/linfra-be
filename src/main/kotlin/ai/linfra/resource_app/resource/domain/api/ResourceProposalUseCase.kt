package ai.linfra.resource_app.resource.domain.api

interface ResourceProposalUseCase {
    fun getResourceSuggestions(threadId: String): String
}
