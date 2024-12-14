package ai.linfra.resource_app.resource.domain.handler

import ai.linfra.resource_app.resource.domain.dependencies.ResourceFileDeletionUseCase
import ai.linfra.resource_app.resource.domain.model.event.DatabaseResourceDeletedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ResourceDeletionHandler(
    private val resourceFileDeletionUseCase: ResourceFileDeletionUseCase
) {

    @EventListener
    fun handleDatabaseResourceDeletionEvent(event: DatabaseResourceDeletedEvent) {
        resourceFileDeletionUseCase.deleteResourceFile(event.resourceId)
    }
}