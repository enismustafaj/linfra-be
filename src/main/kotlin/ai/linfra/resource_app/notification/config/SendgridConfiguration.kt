package ai.linfra.resource_app.notification.config;

import org.springframework.context.annotation.Configuration
import ai.linfra.resource_app.notification.properties.SendgridProperties
import com.sendgrid.SendGrid
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@Configuration
@EnableConfigurationProperties(SendgridProperties::class)
class SengridConfiguration(
    val sendgridProperties: SendgridProperties
) {

    @Bean
    fun createSendgridBean(): SendGrid {
        return SendGrid(sendgridProperties.apiKey)
    }
    
}