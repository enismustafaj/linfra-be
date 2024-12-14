package ai.linfra.resource_app.resource.domain.handler

import ai.linfra.resource_app.resource.domain.model.event.DatabaseResourceDeletedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ResourceDeletionHandler(

) {

    @EventListener
    fun handleDatabaseResourceDeletionEvent(event: DatabaseResourceDeletedEvent) {

    }
}