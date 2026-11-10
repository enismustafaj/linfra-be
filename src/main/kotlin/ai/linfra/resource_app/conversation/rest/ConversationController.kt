package ai.linfra.resource_app.conversation.rest

import ai.linfra.resource_app.conversation.rest.dto.ConversationResponse
import ai.linfra.resource_app.conversation.rest.dto.IntentSummary
import ai.linfra.resource_app.conversation.rest.dto.MessageResponse
import ai.linfra.resource_app.conversation.rest.dto.SendMessageRequest
import ai.linfra.resource_app.conversation.service.ChatService
import ai.linfra.resource_app.conversation.service.ConversationService
import ai.linfra.resource_app.conversation.service.DraftConfirmationService
import ai.linfra.resource_app.tenant.model.TenantContext
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/conversations")
class ConversationController(
    private val conversations: ConversationService,
    private val chat: ChatService,
    private val confirmation: DraftConfirmationService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun start(tenant: TenantContext) = mapOf("id" to conversations.start(tenant.tenantId).id)

    @GetMapping("/{id}")
    fun get(tenant: TenantContext, @PathVariable id: UUID): ConversationResponse {
        conversations.requireOwned(tenant.tenantId, id)
        return ConversationResponse(
            id = id,
            messages = conversations.history(id).map(MessageResponse::of),
            intents = conversations.linkedIntents(id).map(IntentSummary::of),
        )
    }

    /** Returns the assistant's reply. */
    @PostMapping("/{id}/messages")
    fun send(tenant: TenantContext, @PathVariable id: UUID, @RequestBody body: SendMessageRequest) =
        MessageResponse.of(chat.send(tenant.tenantId, id, body.text))

    @PostMapping("/{id}/drafts/{messageId}/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    fun confirm(tenant: TenantContext, @PathVariable id: UUID, @PathVariable messageId: Long) =
        IntentSummary.of(confirmation.confirm(tenant.tenantId, id, messageId))
}
