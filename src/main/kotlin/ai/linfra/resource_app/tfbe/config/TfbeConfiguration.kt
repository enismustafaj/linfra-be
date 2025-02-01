package ai.linfra.resource_app.tfbe.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tfbe")
data class TfbeConfiguration(
    val token: String
)
