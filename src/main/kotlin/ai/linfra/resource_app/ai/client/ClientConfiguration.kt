package ai.linfra.resource_app.ai.client

import ai.linfra.resource_app.ai.configuration.AIModelProperties
import ai.linfra.resource_app.chat.domain.api.GetThreadUseCase
import ai.linfra.resource_app.chat.domain.dependencies.AiConversationModel
import ai.linfra.resource_app.resource.domain.dependencies.AiResourceSuggestionModel
import org.springframework.ai.chat.model.ChatModel
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
    fun aiResourceSuggestionModelClient(
            chatModel: ChatModel,
            getThreadUseCase: GetThreadUseCase
    ): AiResourceSuggestionModel {
        return AiResourceSuggestionModelClientImpl(chatModel, getThreadUseCase)
    }
}
