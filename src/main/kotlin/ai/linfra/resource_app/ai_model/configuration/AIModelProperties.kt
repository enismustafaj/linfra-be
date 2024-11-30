package ai.linfra.resource_app.ai_model.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ai.client")
data class AIModelProperties(
    val url: String,
    val apiKey: String
)
