package ai.linfra.resource_app.intent

import ai.linfra.resource_app.conversation.model.Environment
import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.ranker.RankerFixtures
import ai.linfra.resource_app.terraform.service.TerraformCompiler
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntentPlannerTest : PlanningTest() {

    @Autowired lateinit var compiler: TerraformCompiler

    @Test
    fun `within the default policy an intent is planned, approved and queued in one go`() {
        val intent = plan(proposed(tenant().id))

        assertEquals(IntentState.APPROVED, intent.state)
        assertEquals(compiler.compile(intent).sha256, intent.planHash) // bound to exactly this workspace
        assertEquals("auto-approved by policy", intent.decisionReason)
        assertEquals(1, jobs.findAll().count { it.intentId == intent.id })
    }

    @Test
    fun `above the auto-approve limit it waits for a human, with the reason`() {
        val tenantId = tenant().id
        policy(tenantId, autoMax = "20")

        val intent = plan(proposed(tenantId))

        assertEquals(IntentState.PLANNED, intent.state)
        assertTrue(intent.decisionReason!!.contains("auto-approve limit"), intent.decisionReason)
        assertNull(jobs.findByIntentId(intent.id))
    }

    @Test
    fun `production always waits for a human under the default policy`() {
        val intent = plan(proposed(tenant().id, RankerFixtures.base.copy(environment = Environment.PROD)))

        assertEquals(IntentState.PLANNED, intent.state)
    }

    @Test
    fun `an instance type outside the allow-list is rejected`() {
        val tenantId = tenant().id
        policy(tenantId, types = listOf("dedicated.medium"))

        val intent = plan(proposed(tenantId))

        assertEquals(IntentState.REJECTED, intent.state)
        assertNull(jobs.findByIntentId(intent.id))
    }

    @Test
    fun `approved but not yet created intents count against the budget`() {
        val tenantId = tenant().id
        policy(tenantId, budget = "40")

        val first = plan(proposed(tenantId))
        val second = plan(proposed(tenantId))

        assertEquals(IntentState.APPROVED, first.state)
        assertEquals(IntentState.REJECTED, second.state)
        assertTrue(second.decisionReason!!.contains("over the monthly budget"), second.decisionReason)
    }
}
