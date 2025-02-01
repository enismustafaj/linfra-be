package ai.linfra.resource_app.tfbe.handler

import ai.linfra.resource_app.tfbe.client.TfbeClient
import ai.linfra.resource_app.user.domain.model.event.UserCreatedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class UserCreationHandler(
    val tfbeClient: TfbeClient
) {

    @EventListener
    fun createTfbe(event: UserCreatedEvent) {
        tfbeClient.createtfbe(event.userId)
    }
}