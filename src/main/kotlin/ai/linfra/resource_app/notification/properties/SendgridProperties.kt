package ai.linfra.resource_app.notification.properties;

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "sendgrid")
data class SendgridProperties(
    val apiKey: String
)