package ai.linfra.resource_app.resource.security.config

import ai.linfra.resource_app.resource.security.properties.KeyStoreProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream
import java.security.KeyStore

@Configuration
@EnableConfigurationProperties(KeyStoreProperties::class)
class ResourceKeyStoreConfiguration(
    val keyStoreProperties: KeyStoreProperties
) {

    internal val keyStorePath: String = "src/main/resources/keystore/resourceKeyStore.jks"

    @Bean
    fun createKeyStore(): KeyStore {
        val keyStore: KeyStore = KeyStore.getInstance("JCEKS")

        val keyFileStream: FileInputStream = FileInputStream(keyStorePath)
        keyStore.load(keyFileStream, keyStoreProperties.password.toCharArray())

        return keyStore
    }

}