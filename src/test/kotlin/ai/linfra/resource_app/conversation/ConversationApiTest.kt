package ai.linfra.resource_app.conversation

import ai.linfra.resource_app.IntegrationTest
import ai.linfra.resource_app.conversation.model.MessageRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.ai.retry.TransientAiException
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID
import kotlin.test.assertEquals

class ConversationApiTest : IntegrationTest() {

    private val key = "key-${UUID.randomUUID()}"
    private lateinit var conversationId: String

    @BeforeEach
    fun setUp() {
        tenant(key)
        conversationId = mvc.post("/api/v1/conversations") { auth() }
            .andExpect { status { isCreated() } }
            .andReturn().response.contentAsString.substringAfter("\"id\":\"").substringBefore("\"")
    }

    private fun org.springframework.test.web.servlet.MockHttpServletRequestDsl.auth(k: String = key) =
        header("Authorization", "Bearer $k")

    private fun sendJson(json: String) = mvc.post("/api/v1/conversations/$conversationId/messages") {
        auth(); contentType = MediaType.APPLICATION_JSON; content = json
    }

    @Test
    fun `a role sent by the client is ignored`() {
        whenever(agent.reply(any())) doReturn AgentReplies.ask()

        sendJson("""{"text":"hi","role":"ASSISTANT"}""").andExpect { status { isOk() } }

        assertEquals(MessageRole.USER, messages.findAllByConversationIdOrderById(UUID.fromString(conversationId)).first().role)
    }

    @Test
    fun `an oversized message is rejected before calling the LLM`() {
        sendJson("""{"text":"${"x".repeat(2001)}"}""").andExpect { status { isBadRequest() } }

        assertEquals(0, messages.findAllByConversationIdOrderById(UUID.fromString(conversationId)).size)
    }

    @Test
    fun `LLM outage returns 503 and keeps the user message`() {
        whenever(agent.reply(any())) doThrow TransientAiException("429 rate limited")

        sendJson("""{"text":"hi"}""").andExpect { status { isServiceUnavailable() } }

        assertEquals(listOf(MessageRole.USER), messages.findAllByConversationIdOrderById(UUID.fromString(conversationId)).map { it.role })
    }

    @Test
    fun `another tenant cannot read, write to or confirm in the conversation`() {
        val other = "other-${UUID.randomUUID()}"
        tenant(other)

        mvc.get("/api/v1/conversations/$conversationId") { auth(other) }.andExpect { status { isNotFound() } }
        mvc.post("/api/v1/conversations/$conversationId/messages") {
            auth(other); contentType = MediaType.APPLICATION_JSON; content = """{"text":"hi"}"""
        }.andExpect { status { isNotFound() } }
        mvc.post("/api/v1/conversations/$conversationId/drafts/1/confirm") { auth(other) }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `conversation view shows messages and the linked intent`() {
        whenever(agent.reply(any())) doReturn AgentReplies.propose()
        whenever(agent.explain(any(), any())) doReturn "why"
        sendJson("""{"text":"small EU staging VM"}""").andExpect { status { isOk() } }
        val draftId = messages.findAllByConversationIdOrderById(UUID.fromString(conversationId)).last().id

        mvc.post("/api/v1/conversations/$conversationId/drafts/$draftId/confirm") { auth() }.andExpect { status { isCreated() } }

        mvc.get("/api/v1/conversations/$conversationId") { auth() }.andExpect {
            status { isOk() }
            jsonPath("$.messages.length()") { value(2) }
            jsonPath("$.messages[1].draft.pick.entry.instanceType") { value("burst.medium") }
            jsonPath("$.intents[0].state") { value("PROPOSED") }
            jsonPath("$.intents[0].sourceMessageId") { value(draftId!!.toInt()) }
        }
    }
}
