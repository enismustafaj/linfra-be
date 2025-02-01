package ai.linfra.resource_app.resource.security.domain.api.impl

import ai.linfra.resource_app.resource.security.domain.api.EncryptResourceSecretsUseCase
import ai.linfra.resource_app.resource.security.domain.model.ResourceCredentials
import ai.linfra.resource_app.resource.security.persistence.ResourceSecretsEntity
import ai.linfra.resource_app.resource.security.persistence.ResourceSecretsRepository
import ai.linfra.resource_app.resource.security.properties.KeyStoreProperties
import org.springframework.stereotype.Component
import java.security.KeyStore
import java.util.*
import javax.crypto.Cipher

@Component
class EncryptResourceSecretsImpl(
    val keyStore: KeyStore,
    val keyStoreProperties: KeyStoreProperties,
    val resourceSecretsRepository: ResourceSecretsRepository
): EncryptResourceSecretsUseCase {

    internal val resourceKeyName: String = "resource"

    override fun encryptSecrets(credentials: ResourceCredentials, resourceId: String) {
        val cipher: Cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keyStore.getKey(resourceKeyName, keyStoreProperties.password.toCharArray()))
        val encryptedUsername = cipher.doFinal(credentials.username.toByteArray()).toString()
        val encryptedPassword = cipher.doFinal(credentials.password.toByteArray()).toString()

        resourceSecretsRepository.save(toResourceSecretsEntity(ai.linfra.resource_app.resource.security.persistence.ResourceCredentials(encryptedUsername, encryptedPassword), resourceId))
    }

    private fun toResourceSecretsEntity(secret: ai.linfra.resource_app.resource.security.persistence.ResourceCredentials, resourceId: String): ResourceSecretsEntity {
        return ResourceSecretsEntity(
            resourceId = UUID.fromString(resourceId),
            credentials = secret
        )
    }
}