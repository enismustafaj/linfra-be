package ai.linfra.resource_app.resource.domain.api

import ai.linfra.resource_app.resource.domain.model.ResourceDescription
import ai.linfra.resource_app.resource.domain.model.command.ResourceRemoteUpdateCommand

interface ResourceRemoteUpdateUseCase {
    fun updateResourceStatus(resourceId: String, command: ResourceRemoteUpdateCommand): ResourceDescription
}