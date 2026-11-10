package ai.linfra.resource_app.conversation

import ai.linfra.resource_app.IntegrationTest
import ai.linfra.resource_app.conversation.exception.DraftNotConfirmableException
import ai.linfra.resource_app.conversation.service.ChatService
import ai.linfra.resource_app.conversation.service.ConversationService
import ai.linfra.resource_app.conversation.service.DraftConfirmationService
import ai.linfra.resource_app.intent.model.IntentState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DraftConfirmationTest : IntegrationTest() {

    @Autowired lateinit var conversationService: ConversationService
    @Autowired lateinit var chat: ChatService
    @Autowired lateinit var confirmation: DraftConfirmationService

    private lateinit var tenantId: UUID
    private lateinit var conversationId: UUID

    @BeforeEach
    fun setUp() {
        tenantId = tenant().id
        conversationId = conversationService.start(tenantId).id
        whenever(agent.explain(any(), any())) doReturn "why"
    }

    private fun draftMessageId(): Long {
        whenever(agent.reply(any())) doReturn AgentReplies.propose()
        return chat.send(tenantId, conversationId, "small EU staging VM").id!!
    }

    @Test
    fun `confirming a draft creates a PROPOSED intent linked to the conversation and message`() {
        val messageId = draftMessageId()

        val intent = confirmation.confirm(tenantId, conversationId, messageId)

        assertEquals(IntentState.PROPOSED, intent.state)
        assertEquals(tenantId, intent.tenantId)
        assertEquals(conversationId, intent.conversationId)
        assertEquals(messageId, intent.sourceMessageId)
        assertEquals("burst.medium", ((intent.spec["pick"] as Map<*, *>)["entry"] as Map<*, *>)["instanceType"])
    }

    @Test
    fun `confirming twice returns the same intent`() {
        val messageId = draftMessageId()

        val first = confirmation.confirm(tenantId, conversationId, messageId)
        val second = confirmation.confirm(tenantId, conversationId, messageId)

        assertEquals(first.id, second.id)
        assertEquals(1, intents.findAllByConversationIdOrderByCreatedAt(conversationId).size)
    }

    @Test
    fun `an older draft cannot be confirmed once a newer one exists`() {
        val older = draftMessageId()
        draftMessageId()

        assertFailsWith<DraftNotConfirmableException> { confirmation.confirm(tenantId, conversationId, older) }
    }

    @Test
    fun `a message without a draft cannot be confirmed`() {
        whenever(agent.reply(any())) doReturn AgentReplies.ask()
        val question = chat.send(tenantId, conversationId, "a VM").id!!

        assertFailsWith<DraftNotConfirmableException> { confirmation.confirm(tenantId, conversationId, question) }
    }
}
