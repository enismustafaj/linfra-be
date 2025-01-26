package ai.linfra.resource_app.resource.domain.model.command

import ai.linfra.resource_app.resource.domain.model.ResourceStatusEnum
import com.google.gson.JsonObject

data class ResourceRemoteUpdateCommand(
    val status: ResourceStatusEnum,
    val data: JsonObject
)
