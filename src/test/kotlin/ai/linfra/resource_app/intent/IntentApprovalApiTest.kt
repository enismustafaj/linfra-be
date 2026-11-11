package ai.linfra.resource_app.intent

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.post
import java.util.UUID
import kotlin.test.assertNotNull

class IntentApprovalApiTest : PlanningTest() {

    private val key = "k-${UUID.randomUUID()}"

    private fun post(path: String, k: String = key) = mvc.post(path) { header("Authorization", "Bearer $k") }

    @Test
    fun `plan a proposed intent, then a human approves it`() {
        val tenantId = tenant(key).id
        policy(tenantId, autoMax = "20")
        val intent = proposed(tenantId)

        post("/api/v1/intents/${intent.id}/plan").andExpect {
            status { isOk() }
            jsonPath("$.state") { value("PLANNED") }
        }
        post("/api/v1/intents/${intent.id}/approve").andExpect {
            status { isOk() }
            jsonPath("$.state") { value("APPROVED") }
            jsonPath("$.decisionReason") { value("approved by a human") }
        }
        assertNotNull(jobs.findByIntentId(intent.id))
        post("/api/v1/intents/${intent.id}/approve").andExpect { status { isConflict() } }
    }

    @Test
    fun `a human can reject a waiting intent`() {
        val tenantId = tenant(key).id
        policy(tenantId, autoMax = "20")
        val intent = plan(proposed(tenantId))

        post("/api/v1/intents/${intent.id}/reject").andExpect { jsonPath("$.state") { value("REJECTED") } }
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
    fun `another tenant can't plan, approve or reject`() {
        val intent = plan(proposed(tenant(key).id).also { policy(it.tenantId, autoMax = "20") })
        val other = "o-${UUID.randomUUID()}"
        tenant(other)

        listOf("plan", "approve", "reject").forEach { post("/api/v1/intents/${intent.id}/$it", other).andExpect { status { isNotFound() } } }
    }
}
