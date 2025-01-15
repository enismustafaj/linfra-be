package ai.linfra.resource_app.notification.domain.api.impl;

import ai.linfra.resource_app.notification.domain.api.SendEmailUseCase
import ai.linfra.resource_app.notification.domain.model.EmailPayload
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Email
import com.sendgrid.helpers.mail.objects.Personalization
import org.springframework.stereotype.Component


@Component
class SendEmailImpl(
    val sendGrid: SendGrid
): SendEmailUseCase {

    internal val EMAIL_ENDPOINT: String = "mail/send"

    override fun sendEmail(payload: EmailPayload) {
        sendGrid.api(createEmailRequest(payload))
    }

    private fun createEmailPayload(payload: EmailPayload): Mail {
        val mail: Mail = Mail()
        mail.from = Email(payload.sender)
        mail.templateId = payload.template
        mail.personalization = mutableListOf(createPersonalization(payload.emailData, payload.recipient))
        return mail
    }

    private fun createPersonalization(data: Map<String, Any>, to: String): Personalization {
        val personalization: Personalization = Personalization()
        personalization.addTo(Email(to))

        data.keys.forEach { key ->
            val value = data[key]
            personalization.addDynamicTemplateData(key, value)
        }

        return personalization
    }

    private fun createEmailRequest(payload: EmailPayload): Request {
        val request: Request = Request()
        request.method = Method.POST
        request.endpoint = EMAIL_ENDPOINT;
        request.body = createEmailPayload(payload).build()

        return request
    }
}