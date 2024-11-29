package ai.linfra.resource_app.ai_model.client

import ai.linfra.resource_app.ai_model.configuration.AIModelProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ClientConfiguration {

    @Bean
    fun aiConversationModelClient(modelProperties: AIModelProperties): AiConversationModelClientImpl {
        return AiConversationModelClientImpl(modelProperties)
    }

    @Bean
    fun aiResourceSuggestionModelClient(modelProperties: AIModelProperties): AiResourceSuggestionModelClientImpl {
        return AiResourceSuggestionModelClientImpl(modelProperties)
    }
}