package ai.linfra.resource_app.resource.security.domain.api

import ai.linfra.resource_app.resource.security.domain.model.ResourceCredentials

interface DecryptResourceSecretsUseCase {
    fun decryptResourceSecrets(resourceId: String): ResourceCredentials
}