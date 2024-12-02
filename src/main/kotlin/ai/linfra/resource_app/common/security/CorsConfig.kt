package ai.linfra.resource_app.common.security

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**").allowedMethods("*")
            .allowedOrigins(
                "http://localhost:8080",
                "http://localhost:4200",
                "https://linfra-ai.web.app/",
                "https://linfra.ai"
            )
    }
}
