package ai.linfra.resource_app.notification.domain.api;

import ai.linfra.resource_app.notification.domain.model.EmailPayload

interface SendEmailUseCase {
    fun sendEmail(payload: EmailPayload)
}