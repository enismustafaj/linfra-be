package ai.linfra.resource_app.resource.security.domain.api.impl

import ai.linfra.resource_app.resource.security.domain.api.DecryptResourceSecretsUseCase
import ai.linfra.resource_app.resource.security.domain.exception.NoResourceSecretsException
import ai.linfra.resource_app.resource.security.domain.model.ResourceCredentials
import ai.linfra.resource_app.resource.security.persistence.ResourceSecretsEntity
import ai.linfra.resource_app.resource.security.persistence.ResourceSecretsRepository
import ai.linfra.resource_app.resource.security.properties.KeyStoreProperties
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

    internal val resourceKeyName: String = "resource"

    override fun decryptResourceSecrets(resourceId: String): ResourceCredentials {
        val resourceSecretsEntity: ResourceSecretsEntity = resourceSecretsRepository.findOneByResourceId(UUID.fromString(resourceId))
            ?: throw NoResourceSecretsException("No secrets found for resource with Id $resourceId")

        return decrypt(resourceSecretsEntity.credentials)
    }

    private fun decrypt(secrets: ai.linfra.resource_app.resource.security.persistence.ResourceCredentials): ResourceCredentials {
        val cipher: Cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keyStore.getKey(resourceKeyName, keyStoreProperties.password.toCharArray()))
        val decryptedUsername = cipher.doFinal(secrets.username.toByteArray()).toString()
        val decryptedPassword = cipher.doFinal(secrets.password.toByteArray()).toString()

        return ResourceCredentials(decryptedUsername, decryptedPassword)
    }
}