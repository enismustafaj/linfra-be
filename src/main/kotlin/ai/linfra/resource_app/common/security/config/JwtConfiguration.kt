package ai.linfra.resource_app.common.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
data class JwtConfiguration(
    val jwks: String
) {

}
