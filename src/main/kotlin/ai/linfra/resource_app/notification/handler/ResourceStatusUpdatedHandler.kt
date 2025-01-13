package ai.linfra.resource_app.notification.handler;

import org.springframework.stereotype.Component
import ai.linfra.resource_app.resource.domain.model.event.ResourceRemoteStatusUpdatedEvent

@Component
class ResourceStatusUpdatedHandler {

    fun handleResourceStatusUpdate(event: ResourceRemoteStatusUpdatedEvent): Unit {
        TODO("send a email to the user in case of resource creation");
    }
}