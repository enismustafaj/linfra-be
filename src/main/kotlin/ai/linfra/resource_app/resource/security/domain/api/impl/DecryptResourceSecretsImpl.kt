package ai.linfra.resource_app.resource.security.domain.api.impl

import ai.linfra.resource_app.resource.security.domain.api.DecryptResourceSecretsUseCase
import ai.linfra.resource_app.resource.security.domain.exception.NoResourceSecretsException
import ai.linfra.resource_app.resource.security.persistence.ResourceSecretsEntity
import ai.linfra.resource_app.resource.security.persistence.ResourceSecretsRepository
import ai.linfra.resource_app.resource.security.properties.KeyStoreProperties
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.springframework.stereotype.Component
import java.security.KeyStore
import java.util.*
import javax.crypto.Cipher

@Component
class DecryptResourceSecretsImpl(
    val keyStore: KeyStore,
    val keyStoreProperties: KeyStoreProperties,
    val resourceSecretsRepository: ResourceSecretsRepository
): DecryptResourceSecretsUseCase {

    internal val resourceKeyName: String = ""

    override fun decryptResourceSecrets(resourceId: String): JsonObject {
        val resourceSecretsEntity: ResourceSecretsEntity = resourceSecretsRepository.findOneByResourceId(UUID.fromString(resourceId))
            ?: throw NoResourceSecretsException("No secrets found for resource with Id $resourceId")

        return decrypt(resourceSecretsEntity.secret)
    }

    private fun decrypt(secrets: String): JsonObject {
        val cipher: Cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keyStore.getKey(resourceKeyName, keyStoreProperties.password.toCharArray()))
        val decryptedSecrets = cipher.doFinal(secrets.toByteArray()).toString()

        return JsonParser.parseString(decryptedSecrets).asJsonObject
    }
}