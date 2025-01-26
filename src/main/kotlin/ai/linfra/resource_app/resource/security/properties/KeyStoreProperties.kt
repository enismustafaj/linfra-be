package ai.linfra.resource_app.resource.security.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "keystore")
data class KeyStoreProperties(
    val password: String
)
