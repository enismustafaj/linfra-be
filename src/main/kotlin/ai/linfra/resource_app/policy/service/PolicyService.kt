package ai.linfra.resource_app.policy.service

import ai.linfra.resource_app.conversation.model.Draft
import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.intent.persistence.Intent
import ai.linfra.resource_app.intent.persistence.IntentRepository
import ai.linfra.resource_app.policy.model.PolicyCheck
import ai.linfra.resource_app.policy.model.PolicyDecision
import ai.linfra.resource_app.policy.persistence.Policy
import ai.linfra.resource_app.policy.persistence.PolicyRepository
import ai.linfra.resource_app.resource.model.ResourceStatus
import ai.linfra.resource_app.resource.persistence.ResourceRepository
import ai.linfra.resource_app.policy.exception.InvalidPolicyException
import ai.linfra.resource_app.policy.model.PolicyUpdate
import ai.linfra.resource_app.ranker.service.PriceCatalog
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

/** Loads the tenant's policy and committed spend, and asks the evaluator. Call inside the caller's transaction. */
@Service
class PolicyService(
    private val policies: PolicyRepository,
    private val intents: IntentRepository,
    private val resources: ResourceRepository,
    private val evaluator: PolicyEvaluator,
    private val objectMapper: ObjectMapper,
    private val catalog: PriceCatalog,
    @Value("\${linfra.policy.monthly-budget-eur}") private val defaultBudget: BigDecimal,
    @Value("\${linfra.policy.auto-approve-max-eur}") private val defaultAutoApproveMax: BigDecimal,
    @Value("\${linfra.policy.require-approval-for-prod}") private val defaultRequireProdApproval: Boolean,
) {
    fun hasOwnPolicy(tenantId: UUID): Boolean = policies.existsById(tenantId)

    /**
     * Saves the tenant's policy. Types and regions must exist in the catalog (empty means any); it only affects
     * decisions made from now on. Intents waiting for a human are checked against it when they're approved.
     */
    @Transactional
    fun update(tenantId: UUID, update: PolicyUpdate): Policy {
        val unknownTypes = update.allowedInstanceTypes - catalog.entries.map { it.instanceType }.toSet()
        val unknownRegions = update.allowedRegions - catalog.entries.map { it.region }.toSet()
        if (unknownTypes.isNotEmpty()) throw InvalidPolicyException("unknown instance types: ${unknownTypes.joinToString()}")
        if (unknownRegions.isNotEmpty()) throw InvalidPolicyException("unknown regions: ${unknownRegions.joinToString()}")
        if (update.monthlyBudgetEur <= BigDecimal.ZERO) throw InvalidPolicyException("monthly budget must be positive")
        if (update.autoApproveMaxEur < BigDecimal.ZERO || update.autoApproveMaxEur > update.monthlyBudgetEur) {
            throw InvalidPolicyException("auto-approve limit must be between 0 and the monthly budget")
        }
        val policy = policies.findById(tenantId).orElseGet { policyFor(tenantId) }.apply {
            allowedInstanceTypes = update.allowedInstanceTypes.distinct().sorted()
            allowedRegions = update.allowedRegions.distinct().sorted()
            monthlyBudgetEur = update.monthlyBudgetEur
            autoApproveMaxEur = update.autoApproveMaxEur
            requireApprovalForProd = update.requireApprovalForProd
            updatedAt = Instant.now()
        }
        return policies.save(policy)
    }

    fun policyFor(tenantId: UUID): Policy = policies.findById(tenantId).orElseGet {
        Policy(
            tenantId = tenantId,
            monthlyBudgetEur = defaultBudget,
            autoApproveMaxEur = defaultAutoApproveMax,
            requireApprovalForProd = defaultRequireProdApproval,
        )
    }

    /** [allowAutoApprove] false when a human is approving: only the hard rules apply then. */
    fun decide(intent: Intent, allowAutoApprove: Boolean = true): PolicyDecision {
        val draft = draft(intent)
        val check = PolicyCheck(
            instanceType = draft.pick.entry.instanceType,
            region = draft.pick.entry.region,
            environment = draft.requirements.environment,
            monthlyCostEur = draft.pick.cost.total,
            committedSpendEur = committedSpend(intent.tenantId, excluding = intent.id),
        )
        return evaluator.evaluate(policyFor(intent.tenantId), check, allowAutoApprove)
    }

    /**
     * Running resources plus approved intents not yet created: counting only running resources would let two
     * approvals in quick succession both fit under the same remaining budget.
     */
    fun committedSpend(tenantId: UUID, excluding: UUID? = null): BigDecimal {
        val inFlight = intents.findAllByTenantIdAndStateIn(tenantId, listOf(IntentState.APPROVED, IntentState.APPLYING))
        val running = resources.findAllByTenantIdAndStatusIn(tenantId, listOf(ResourceStatus.ACTIVE, ResourceStatus.MISSING))
            .map { it.intentId }
        val counted = (inFlight + intents.findAllById(running)).distinctBy { it.id }.filter { it.id != excluding }
        return counted.fold(BigDecimal.ZERO) { sum, i -> sum + draft(i).pick.cost.total }
    }

    private fun draft(intent: Intent): Draft = objectMapper.convertValue(intent.spec, Draft::class.java)
}
