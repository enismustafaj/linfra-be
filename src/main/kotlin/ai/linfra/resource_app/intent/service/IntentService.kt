package ai.linfra.resource_app.intent.service

import ai.linfra.resource_app.intent.exception.IntentNotFoundException
import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.intent.persistence.Intent
import ai.linfra.resource_app.intent.persistence.IntentRepository
import ai.linfra.resource_app.outbox.persistence.OutboxJob
import ai.linfra.resource_app.outbox.persistence.OutboxJobRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class IntentService(
    private val intents: IntentRepository,
    private val jobs: OutboxJobRepository,
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
}
