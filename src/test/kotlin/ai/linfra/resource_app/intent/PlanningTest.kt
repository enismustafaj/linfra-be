package ai.linfra.resource_app.intent

import ai.linfra.resource_app.IntegrationTest
import ai.linfra.resource_app.conversation.model.Draft
import ai.linfra.resource_app.conversation.model.VmRequirements
import ai.linfra.resource_app.intent.persistence.Intent
import ai.linfra.resource_app.intent.service.IntentPlanner
import ai.linfra.resource_app.policy.persistence.Policy
import ai.linfra.resource_app.policy.persistence.PolicyRepository
import ai.linfra.resource_app.ranker.RankerFixtures
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.util.UUID

/** Intents with real draft specs (burst.medium in local-eu, €25.40/month), planned through the real planner. */
abstract class PlanningTest : IntegrationTest() {

    @Autowired lateinit var planner: IntentPlanner
    @Autowired lateinit var policies: PolicyRepository
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var tx: TransactionTemplate

    fun proposed(tenantId: UUID, requirements: VmRequirements = RankerFixtures.base): Intent {
        val draft = Draft(requirements, RankerFixtures.ranker.pick(requirements)!!, "why")
        return intents.save(Intent(tenantId = tenantId, spec = objectMapper.convertValue(draft)))
    }

    fun plan(intent: Intent): Intent = tx.execute { planner.planAndDecide(intents.findById(intent.id).get()) }!!

    fun policy(tenantId: UUID, budget: String = "200", autoMax: String = "50", types: List<String> = emptyList()) =
        policies.save(Policy(tenantId, types, emptyList(), BigDecimal(budget), BigDecimal(autoMax), requireApprovalForProd = true))
}
