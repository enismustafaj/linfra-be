package ai.linfra.resource_app.ai_model.client

import ai.linfra.resource_app.ai_model.configuration.AIModelProperties
import ai.linfra.resource_app.chat.domain.dependencies.AiConversationModel
import ai.linfra.resource_app.resource.domain.dependencies.AiResourceSuggestionModel
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AIModelProperties::class)
class ClientConfiguration {

    @Bean
    fun aiConversationModelClient(modelProperties: AIModelProperties): AiConversationModel {
        return AiConversationModelClientImpl(modelProperties)
    }

    @Bean
    fun aiResourceSuggestionModelClient(modelProperties: AIModelProperties): AiResourceSuggestionModel {
        return AiResourceSuggestionModelClientImpl(modelProperties)
    }
}