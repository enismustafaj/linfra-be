package ai.linfra.resource_app.ai_model.client

import ai.linfra.resource_app.ai_model.configuration.AIModelProperties
import ai.linfra.resource_app.resource.domain.dependencies.AiResourceSuggestionModel

class AiResourceSuggestionModelClientImpl(
    private val modelProperties: AIModelProperties
): AiResourceSuggestionModel {
}