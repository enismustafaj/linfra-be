package ai.linfra.resource_app.resource.domain.dependencies

interface AiResourceSuggestionModel {
    fun suggestResource(): String
}