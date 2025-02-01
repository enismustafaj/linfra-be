package ai.linfra.resource_app.resource.security.domain.api

import ai.linfra.resource_app.resource.security.domain.model.ResourceCredentials


interface EncryptResourceSecretsUseCase {

    fun encryptSecrets(credentials: ResourceCredentials, resourceId: String): Unit
}