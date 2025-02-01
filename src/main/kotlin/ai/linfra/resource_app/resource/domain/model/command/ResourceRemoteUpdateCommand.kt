package ai.linfra.resource_app.resource.domain.model.command

import ai.linfra.resource_app.resource.domain.model.ResourceStatusEnum
import ai.linfra.resource_app.resource.security.domain.model.ResourceCredentials
import com.google.gson.JsonObject

data class ResourceRemoteUpdateCommand(
    val status: ResourceStatusEnum,
    val url: String? = null,
    val credentials: ResourceCredentials? = null
)
