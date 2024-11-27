package ai.linfra.resource_app.ai_model.client

import ai.linfra.resource_app.ai_model.configuration.AIModelProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ClientConfiguration {

    @Bean
    fun aiModelClient(modelProperties: AIModelProperties): AiModelClient {
        return AiModelClientImpl(modelProperties)
    }
}