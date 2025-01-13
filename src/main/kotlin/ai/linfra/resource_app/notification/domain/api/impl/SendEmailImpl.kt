package ai.linfra.resource_app.notification.domain.api.impl;

import ai.linfra.resource_app.notification.domain.api.SendEmailUseCase
import ai.linfra.resource_app.notification.domain.model.EmailPayload
import org.springframework.stereotype.Component

@Component
class SendEmailImpl(): SendEmailUseCase {

    override fun sendEmail(payload: EmailPayload) {


        TODO("sen email to user whn sendgrid is integrated")
     }

}