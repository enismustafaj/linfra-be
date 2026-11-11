package ai.linfra.resource_app.policy

import ai.linfra.resource_app.conversation.model.Environment
import ai.linfra.resource_app.policy.model.PolicyCheck
import ai.linfra.resource_app.policy.model.PolicyDecision
import ai.linfra.resource_app.policy.persistence.Policy
import ai.linfra.resource_app.policy.service.PolicyEvaluator
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PolicyEvaluatorTest {

    private val evaluator = PolicyEvaluator()

    private fun policy(types: List<String> = emptyList(), regions: List<String> = emptyList(), budget: String = "200", autoMax: String = "50") =
        Policy(UUID.randomUUID(), types, regions, BigDecimal(budget), BigDecimal(autoMax), requireApprovalForProd = true)

    private fun check(cost: String = "25.40", committed: String = "0", env: Environment = Environment.STAGING, type: String = "burst.medium", region: String = "local-eu") =
        PolicyCheck(type, region, env, BigDecimal(cost), BigDecimal(committed))

    @Test
    fun `within every limit is auto-approved`() {
        assertEquals(PolicyDecision.AutoApprove, evaluator.evaluate(policy(), check()))
    }

    @Test
    fun `a type or region outside the allow-list is rejected`() {
        assertIs<PolicyDecision.Reject>(evaluator.evaluate(policy(types = listOf("dedicated.medium")), check()))
        assertIs<PolicyDecision.Reject>(evaluator.evaluate(policy(regions = listOf("local-us")), check()))
    }

    @Test
    fun `the budget counts what is already committed`() {
        val decision = evaluator.evaluate(policy(budget = "40"), check(committed = "25.40"))

        assertIs<PolicyDecision.Reject>(decision)
        assertTrue(decision.reason.contains("€50.80 > €40"), decision.reason)
    }

    @Test
    fun `above the auto-approve limit or in production a human decides`() {
        assertIs<PolicyDecision.NeedsApproval>(evaluator.evaluate(policy(autoMax = "20"), check()))
        assertIs<PolicyDecision.NeedsApproval>(evaluator.evaluate(policy(), check(env = Environment.PROD)))
    }

    @Test
    fun `a human approval skips the soft rules but never the hard ones`() {
        assertEquals(PolicyDecision.AutoApprove, evaluator.evaluate(policy(autoMax = "20"), check(env = Environment.PROD), allowAutoApprove = false))
        assertIs<PolicyDecision.Reject>(evaluator.evaluate(policy(budget = "10"), check(), allowAutoApprove = false))
    }
}
