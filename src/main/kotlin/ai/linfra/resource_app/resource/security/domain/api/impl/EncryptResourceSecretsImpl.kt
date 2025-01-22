package ai.linfra.resource_app.resource.security.domain.api.impl

import ai.linfra.resource_app.resource.security.domain.api.EncryptResourceSecretsUseCase
import ai.linfra.resource_app.resource.security.persistence.ResourceSecretsEntity
import ai.linfra.resource_app.resource.security.persistence.ResourceSecretsRepository
import ai.linfra.resource_app.resource.security.properties.KeyStoreProperties
import com.google.gson.JsonObject
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

    internal val resourceKeyName: String = ""

    override fun encryptSecrets(secrets: JsonObject, resourceId: String) {
        val cipher: Cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keyStore.getKey(resourceKeyName, keyStoreProperties.password.toCharArray()))
        val encryptedSecrets = cipher.doFinal(secrets.toString().toByteArray()).toString()

        resourceSecretsRepository.save(toResourceSecretsEntity(encryptedSecrets, resourceId))
    }

    private fun toResourceSecretsEntity(secret: String, resourceId: String): ResourceSecretsEntity {
        return ResourceSecretsEntity(
            resourceId = UUID.fromString(resourceId),
            secret = secret
        )
    }
}