package ai.linfra.resource_app.conversation

import ai.linfra.resource_app.IntegrationTest
import ai.linfra.resource_app.conversation.exception.InvalidAgentOutputException
import ai.linfra.resource_app.conversation.model.MessageRole
import ai.linfra.resource_app.conversation.service.ChatService
import ai.linfra.resource_app.conversation.service.ConversationService
import ai.linfra.resource_app.ranker.model.Provider
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ChatServiceTest : IntegrationTest() {

    @Autowired lateinit var conversationService: ConversationService
    @Autowired lateinit var chat: ChatService

    private val tenantId by lazy { tenant().id }

    private fun sendInNewConversation(text: String = "I need a VM") = chat.send(tenantId, conversationService.start(tenantId).id, text)

    @Test
    fun `a question from the agent is stored without a draft`() {
        whenever(agent.reply(any())) doReturn AgentReplies.ask()

        val reply = sendInNewConversation()

        assertEquals(MessageRole.ASSISTANT, reply.role)
        assertEquals("Which environment is this for?", reply.content)
        assertNull(reply.draft)
    }

    @Test
    fun `a valid proposal becomes a draft with the ranker's pick`() {
        whenever(agent.reply(any())) doReturn AgentReplies.propose()
        whenever(agent.explain(any(), any())) doReturn "Cheapest EU option."

        val draft = assertNotNull(sendInNewConversation().draft)

        assertEquals(Provider.DOCKER, draft.pick.entry.provider)
        assertEquals("burst.medium", draft.pick.entry.instanceType)
        assertEquals("Cheapest EU option.", draft.explanation)
    }

    @Test
    fun `the user message is stored before the agent replies`() {
        whenever(agent.reply(any())) doReturn AgentReplies.ask()

        val reply = sendInNewConversation("hello")

        assertEquals(
            listOf(MessageRole.USER to "hello", MessageRole.ASSISTANT to reply.content),
            conversationService.history(reply.conversationId).map { it.role to it.content },
        )
    }

    @Test
    fun `out-of-range requirements never become a draft`() {
        whenever(agent.reply(any())) doReturn AgentReplies.propose(AgentReplies.smallEuStaging.copy(vcpuMin = 512))

        val reply = sendInNewConversation()

        assertNull(reply.draft)
        assert(reply.content.contains("vcpuMin")) { reply.content }
    }

    @Test
    fun `PROPOSE without requirements never becomes a draft`() {
        whenever(agent.reply(any())) doReturn AgentReplies.propose(requirements = null)

        assertNull(sendInNewConversation().draft)
    }

    @Test
    fun `unparseable agent output asks the user to rephrase`() {
        whenever(agent.reply(any())) doThrow InvalidAgentOutputException("not json")

        val reply = sendInNewConversation()

        assertNull(reply.draft)
        assert(reply.content.contains("rephrase")) { reply.content }
    }

    @Test
    fun `nothing in the catalog fits means no draft`() {
        whenever(agent.reply(any())) doReturn AgentReplies.propose(AgentReplies.smallEuStaging.copy(maxMonthlyEur = 1.toBigDecimal()))

        assertNull(sendInNewConversation().draft)
    }

    @Test
    fun `a failed explanation call still yields the draft`() {
        whenever(agent.reply(any())) doReturn AgentReplies.propose()
        whenever(agent.explain(any(), any())) doThrow RuntimeException("timeout")

        val draft = assertNotNull(sendInNewConversation().draft)

        assert(draft.explanation.contains("burst.medium")) { draft.explanation }
    }
}
