package ai.linfra.resource_app.ai.client

import ai.linfra.resource_app.ai.configuration.AIModelProperties
import ai.linfra.resource_app.resource.domain.dependencies.AiResourceSuggestionModel

class AiResourceSuggestionModelClientImpl(
    private val modelProperties: AIModelProperties
): AiResourceSuggestionModel {

    override fun suggestResource(): String {
        TODO("Not yet implemented")
    }
}