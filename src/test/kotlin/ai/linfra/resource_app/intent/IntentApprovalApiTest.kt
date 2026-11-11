package ai.linfra.resource_app.intent

import ai.linfra.resource_app.intent.model.IntentState
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.post
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** One click: approving plans the intent first if needed, and the policy still has the last word on hard rules. */
class IntentApprovalApiTest : PlanningTest() {

    private val key = "k-${UUID.randomUUID()}"

    private fun post(path: String, k: String = key) = mvc.post(path) { header("Authorization", "Bearer $k") }

    @Test
    fun `approving a proposed intent within policy auto-approves it`() {
        val intent = proposed(tenant(key).id)

        post("/api/v1/intents/${intent.id}/approve").andExpect {
            status { isOk() }
            jsonPath("$.state") { value("APPROVED") }
            jsonPath("$.decisionReason") { value("auto-approved by policy") }
        }
        assertNotNull(jobs.findByIntentId(intent.id))
    }

    @Test
    fun `approving a proposed intent above the auto-approve limit is the human approval, in one step`() {
        val tenantId = tenant(key).id
        policy(tenantId, autoMax = "20")
        val intent = proposed(tenantId)

        post("/api/v1/intents/${intent.id}/approve").andExpect {
            jsonPath("$.state") { value("APPROVED") }
            jsonPath("$.decisionReason") { value("approved by a human") }
        }
        assertNotNull(jobs.findByIntentId(intent.id))
    }

    @Test
    fun `approving can't override a hard rule`() {
        val tenantId = tenant(key).id
        policy(tenantId, types = listOf("dedicated.medium"))
        val intent = proposed(tenantId)

        post("/api/v1/intents/${intent.id}/approve").andExpect { jsonPath("$.state") { value("REJECTED") } }
        assertNull(jobs.findByIntentId(intent.id))
    }

    @Test
    fun `an expired intent is re-planned and approved in one step`() {
        val intent = proposed(tenant(key).id)
        intents.findById(intent.id).get().apply { state = IntentState.EXPIRED; intents.save(this) }

        post("/api/v1/intents/${intent.id}/approve").andExpect { jsonPath("$.state") { value("APPROVED") } }
    }

    @Test
    fun `an intent already approved can't be approved again`() {
        val intent = plan(proposed(tenant(key).id))
        assertEquals(IntentState.APPROVED, intent.state)

        post("/api/v1/intents/${intent.id}/approve").andExpect { status { isConflict() } }
    }

    @Test
    fun `a proposed or waiting intent can be rejected, an approved one can't`() {
        val tenantId = tenant(key).id
        val proposed = proposed(tenantId)
        val approved = plan(proposed(tenantId))

        post("/api/v1/intents/${proposed.id}/reject").andExpect { jsonPath("$.state") { value("REJECTED") } }
        post("/api/v1/intents/${approved.id}/reject").andExpect { status { isConflict() } }
    }

    @Test
    fun `a human approval still respects the budget`() {
        val tenantId = tenant(key).id
        policy(tenantId, budget = "30", autoMax = "20")
        val a = plan(proposed(tenantId))
        val b = plan(proposed(tenantId))

        post("/api/v1/intents/${b.id}/approve").andExpect { jsonPath("$.state") { value("APPROVED") } }
        post("/api/v1/intents/${a.id}/approve").andExpect { jsonPath("$.state") { value("REJECTED") } }
    }

    @Test
    fun `another tenant can't approve or reject`() {
        val intent = proposed(tenant(key).id)
        val other = "o-${UUID.randomUUID()}"
        tenant(other)

        listOf("approve", "reject").forEach { post("/api/v1/intents/${intent.id}/$it", other).andExpect { status { isNotFound() } } }
    }
}
