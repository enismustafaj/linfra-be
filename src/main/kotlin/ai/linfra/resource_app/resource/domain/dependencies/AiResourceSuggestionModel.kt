package ai.linfra.resource_app.resource.domain.dependencies

import ai.linfra.resource_app.resource.domain.model.ResourceSuggestion

interface AiResourceSuggestionModel {
    fun suggestResource(threadId: String): ResourceSuggestion?
}
