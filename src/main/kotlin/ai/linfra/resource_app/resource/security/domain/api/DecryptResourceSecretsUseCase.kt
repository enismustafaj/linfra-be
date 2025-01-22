package ai.linfra.resource_app.resource.security.domain.api

import com.google.gson.JsonObject

interface DecryptResourceSecretsUseCase {
    fun decryptResourceSecrets(resourceId: String): JsonObject
}