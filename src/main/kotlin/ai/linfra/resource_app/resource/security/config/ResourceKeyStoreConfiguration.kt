package ai.linfra.resource_app.resource.security.config

import ai.linfra.resource_app.resource.security.properties.KeyStoreProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.ResourceLoader
import java.io.FileInputStream
import java.io.InputStream
import java.security.KeyStore

@Configuration
@EnableConfigurationProperties(KeyStoreProperties::class)
class ResourceKeyStoreConfiguration(
    val keyStoreProperties: KeyStoreProperties,
    val resourceLoader: ResourceLoader
) {

    @Bean
    fun createKeyStore(): KeyStore {
        val keyStore: KeyStore = KeyStore.getInstance("JCEKS")

        val keyFileStream: InputStream = resourceLoader.getResource("classpath:keystore/resourceKeyStore.jks").inputStream
        keyStore.load(keyFileStream, keyStoreProperties.password.toCharArray())

        return keyStore
    }

}