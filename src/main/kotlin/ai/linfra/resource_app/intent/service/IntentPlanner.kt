package ai.linfra.resource_app.intent.service

import ai.linfra.resource_app.intent.exception.IntentNotActionableException
import ai.linfra.resource_app.intent.exception.IntentNotFoundException
import ai.linfra.resource_app.intent.persistence.IntentRepository
import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.intent.persistence.Intent
import ai.linfra.resource_app.policy.model.PolicyDecision
import ai.linfra.resource_app.policy.service.PolicyService
import ai.linfra.resource_app.tenant.persistence.TenantRepository
import ai.linfra.resource_app.terraform.exception.NotInAllowListException
import ai.linfra.resource_app.terraform.service.TerraformCompiler
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.UUID

/**
 * Plan, then policy: PROPOSED (or EXPIRED) → PLANNED with a plan hash, then auto-approved (APPROVED + outbox job),
 * left PLANNED for a human, or REJECTED. Joins the caller's transaction, so confirming a draft and everything up to
 * the outbox job commit together.
 */
@Service
class IntentPlanner(
    private val compiler: TerraformCompiler,
    private val policy: PolicyService,
    private val intentService: IntentService,
    private val tenants: TenantRepository,
    private val intents: IntentRepository,
    @Value("\${linfra.policy.approval-ttl}") private val approvalTtl: Duration,
) {
    /**
     * A user approves an intent from the UI, whatever stage it's at, in one step. Not planned yet: it's planned and the
     * policy decides first, so within policy it's auto-approved and a hard-rule breach is rejected. Still waiting
     * after that (above the auto-approve limit, or production): the user's click is the human approval.
     */
    @Transactional
    fun approve(tenantId: UUID, intentId: UUID): Intent {
        val intent = intents.findByIdAndTenantId(intentId, tenantId) ?: throw IntentNotFoundException(intentId)
        if (intent.state == IntentState.PROPOSED || intent.state == IntentState.EXPIRED) {
            planAndDecide(intent)
            if (intent.state != IntentState.PLANNED) return intent
        }
        return intentService.approveByHuman(tenantId, intentId)
    }

    @Transactional
    fun planAndDecide(intent: Intent): Intent {
        if (intent.state != IntentState.PROPOSED && intent.state != IntentState.EXPIRED) {
            throw IntentNotActionableException("only PROPOSED or EXPIRED intents can be planned; this one is ${intent.state}")
        }
        tenants.lockById(intent.tenantId) // one budget decision per tenant at a time

        val plan = try {
            compiler.compile(intent)
        } catch (e: NotInAllowListException) {
            intent.moveTo(IntentState.REJECTED)
            intent.decisionReason = "can't be planned: ${e.message}"
            return intent
        }
        intent.moveTo(IntentState.PLANNED)
        intent.planHash = plan.sha256

        when (val decision = policy.decide(intent)) {
            PolicyDecision.AutoApprove -> {
                intent.decisionReason = "auto-approved by policy"
                intentService.approve(intent.tenantId, intent.id, plan.sha256, approvalTtl)
            }
            is PolicyDecision.NeedsApproval -> intent.decisionReason = decision.reason
            is PolicyDecision.Reject -> {
                intent.moveTo(IntentState.REJECTED)
                intent.decisionReason = decision.reason
            }
        }
        return intent
    }
}
