package ai.linfra.resource_app.notification.handler;

import org.springframework.stereotype.Component
import ai.linfra.resource_app.resource.domain.model.event.ResourceRemoteStatusUpdatedEvent
import ai.linfra.resource_app.resource.domain.api.GetResourceUseCase
import ai.linfra.resource_app.resource.domain.model.ResourceDescription
import ai.linfra.resource_app.notification.domain.api.SendEmailUseCase
import ai.linfra.resource_app.notification.domain.model.EmailPayload
import ai.linfra.resource_app.user.domain.api.GetUserUseCase
import ai.linfra.resource_app.user.domain.model.User

@Component
class ResourceStatusUpdatedHandler(
    val sendEmailUseCase: SendEmailUseCase,
    val getUserUseCase: GetUserUseCase,
    val getResourceUseCase: GetResourceUseCase
) {

    internal val templatedId: String = ""
    internal val senderEmail: String = "linfra@ai.com"

    fun handleResourceStatusUpdate(event: ResourceRemoteStatusUpdatedEvent): Unit {
        sendEmailUseCase.sendEmail(createEmailPayload(event));
    }

    private fun createEmailPayload(event: ResourceRemoteStatusUpdatedEvent): EmailPayload {
        val resource: ResourceDescription = getResourceUseCase.getResource(event.resourceId)
        val user: User = getUserUseCase.getUserById(resource.userId!!);

        return EmailPayload(senderEmail, user.email, templatedId, event.metadata!!)
    }
}