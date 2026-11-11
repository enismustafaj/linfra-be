package ai.linfra.resource_app.intent.service

import ai.linfra.resource_app.intent.exception.IntentNotActionableException
import ai.linfra.resource_app.intent.exception.IntentNotFoundException
import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.intent.persistence.Intent
import ai.linfra.resource_app.intent.persistence.IntentRepository
import ai.linfra.resource_app.outbox.persistence.OutboxJob
import ai.linfra.resource_app.outbox.persistence.OutboxJobRepository
import ai.linfra.resource_app.policy.model.PolicyDecision
import ai.linfra.resource_app.policy.service.PolicyService
import ai.linfra.resource_app.tenant.persistence.TenantRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class IntentService(
    private val intents: IntentRepository,
    private val jobs: OutboxJobRepository,
    private val policy: PolicyService,
    private val tenants: TenantRepository,
    @Value("\${linfra.policy.approval-ttl}") private val approvalTtl: Duration,
) {

    /**
     * Approval and the outbox job commit in ONE transaction: either the intent is APPROVED and a worker
     * will pick it up, or neither happened. The approval is bound to the plan hash it was given for.
     */
    @Transactional
    fun approve(tenantId: UUID, intentId: UUID, planHash: String, ttl: Duration): Intent {
        val intent = intents.findByIdAndTenantId(intentId, tenantId) ?: throw IntentNotFoundException(intentId)
        check(intent.planHash == planHash) { "Approval is for plan $planHash but intent $intentId has plan ${intent.planHash}" }

        val now = Instant.now()
        intent.moveTo(IntentState.APPROVED)
        intent.approvedAt = now
        intent.approvalExpiresAt = now.plus(ttl)
        jobs.save(OutboxJob(intentId = intent.id))
        return intent
    }

    /** A human approves a planned intent the policy left waiting. The hard rules are checked again: spend may have changed. */
    @Transactional
    fun approveByHuman(tenantId: UUID, intentId: UUID): Intent {
        val intent = waitingForApproval(tenantId, intentId)
        tenants.lockById(tenantId)
        when (val decision = policy.decide(intent, allowAutoApprove = false)) {
            is PolicyDecision.Reject -> {
                intent.moveTo(IntentState.REJECTED)
                intent.decisionReason = decision.reason
            }
            else -> {
                intent.decisionReason = "approved by a human"
                approve(tenantId, intentId, intent.planHash!!, approvalTtl)
            }
        }
        return intent
    }

    /** A human turns down an intent that is waiting for a decision, planned or not. */
    @Transactional
    fun rejectByHuman(tenantId: UUID, intentId: UUID): Intent {
        val intent = intents.findByIdAndTenantId(intentId, tenantId) ?: throw IntentNotFoundException(intentId)
        if (intent.state != IntentState.PLANNED && intent.state != IntentState.PROPOSED) {
            throw IntentNotActionableException("intent is ${intent.state}; only proposed or planned intents can be rejected")
        }
        intent.moveTo(IntentState.REJECTED)
        intent.decisionReason = "rejected by a human"
        return intent
    }

    private fun waitingForApproval(tenantId: UUID, intentId: UUID): Intent {
        val intent = intents.findByIdAndTenantId(intentId, tenantId) ?: throw IntentNotFoundException(intentId)
        if (intent.state != IntentState.PLANNED) throw IntentNotActionableException("intent is ${intent.state}, not waiting for approval")
        return intent
    }
}
