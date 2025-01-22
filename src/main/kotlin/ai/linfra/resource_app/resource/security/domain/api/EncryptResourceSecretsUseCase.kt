package ai.linfra.resource_app.resource.security.domain.api

import com.google.gson.JsonObject

interface EncryptResourceSecretsUseCase {

    fun encryptSecrets(secrets: JsonObject, resourceId: String): Unit
}