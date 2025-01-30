package ai.linfra.resource_app.firebase.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.ByteArrayInputStream
import java.util.*


@Configuration
@EnableConfigurationProperties(FirebaseProperties::class)
class FirebaseConfiguration(
    val firebaseProperties: FirebaseProperties
) {

    fun firebaseOptions(): FirebaseOptions {
        val fromStream = GoogleCredentials.fromStream(
            ByteArrayInputStream(
                Base64.getDecoder().decode(firebaseProperties.credentials.toByteArray())
            )
        )
        val options = FirebaseOptions.builder()
            .setCredentials(fromStream)
            .build()

        return options;
    }

    @Bean
    fun firebaseApp(): FirebaseApp {
        return FirebaseApp.initializeApp(firebaseOptions())
    }
}