package ai.linfra.resource_app.outbox.service

import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.intent.persistence.IntentRepository
import ai.linfra.resource_app.outbox.exception.LostLeaseException
import ai.linfra.resource_app.outbox.model.Claim
import ai.linfra.resource_app.outbox.model.JobStatus
import ai.linfra.resource_app.outbox.persistence.OutboxJob
import ai.linfra.resource_app.outbox.persistence.OutboxJobRepository
import ai.linfra.resource_app.outbox.persistence.Run
import ai.linfra.resource_app.outbox.persistence.RunRepository
import ai.linfra.resource_app.provider.model.ProvisionedResource
import ai.linfra.resource_app.resource.persistence.Resource
import ai.linfra.resource_app.resource.persistence.ResourceRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

/**
 * All outbox state changes, each in its own short transaction. No cloud calls happen in here, so a slow
 * provider never holds a DB connection or a row lock.
 */
@Service
class OutboxStore(
    private val jobs: OutboxJobRepository,
    private val intents: IntentRepository,
    private val runs: RunRepository,
    private val resources: ResourceRepository,
) {

    @Transactional
    fun claim(workerId: String, lease: Duration): Claim? {
        val now = Instant.now()
        val job = jobs.findClaimable(now, PageRequest.of(0, 1)).firstOrNull() ?: return null
        val jobId = job.id!!
        val intent = intents.findById(job.intentId).orElseThrow()

        if (intent.state == IntentState.APPROVED && intent.approvalExpiresAt!!.isBefore(now)) {
            intent.moveTo(IntentState.EXPIRED)
            job.status = JobStatus.DONE
            job.lastError = "approval expired before apply started"
            return null
        }
        if (intent.state == IntentState.APPROVED) intent.moveTo(IntentState.APPLYING)
        // Still APPLYING here means the previous worker's lease ran out mid-apply. We take over;
        // the worker calls find(intent) before applying, so it adopts what the dead worker created.
        if (intent.state != IntentState.APPLYING) {
            job.status = JobStatus.DEAD
            job.lastError = "job claimable but intent is ${intent.state}"
            return null
        }

        job.status = JobStatus.LEASED
        job.leaseOwner = workerId
        job.leaseUntil = now.plus(lease)
        job.fencingToken += 1
        job.attempts += 1
        runs.save(Run(intentId = intent.id, jobId = jobId, fencingToken = job.fencingToken, workerId = workerId, startedAt = now))

        return Claim(jobId, intent.id, intent.tenantId, intent.spec, job.fencingToken, job.attempts)
    }

    /** Returns false once another worker has taken the job over. */
    @Transactional
    fun heartbeat(claim: Claim, lease: Duration): Boolean {
        val job = lockIfStillOwned(claim) ?: return false
        job.leaseUntil = Instant.now().plus(lease)
        return true
    }

    /**
     * The resource row and SUCCEEDED commit together, so they can never disagree. If the cloud id is already
     * recorded for another intent, the unique constraint fails this transaction and the job goes down the failure
     * path: an adopted resource must never be linked to two intents.
     */
    @Transactional
    fun succeed(claim: Claim, provisioned: ProvisionedResource) {
        val job = lockIfStillOwned(claim) ?: throw LostLeaseException(claim)
        job.status = JobStatus.DONE
        job.leaseOwner = null
        job.leaseUntil = null

        resources.saveAndFlush(
            Resource(
                tenantId = claim.tenantId,
                intentId = claim.intentId,
                provider = provisioned.provider,
                region = provisioned.region,
                instanceType = provisioned.instanceType,
                cloudId = provisioned.cloudId,
                outputs = provisioned.outputs,
            )
        )
        intents.findById(claim.intentId).orElseThrow().moveTo(IntentState.SUCCEEDED)
        finishRun(claim, "SUCCEEDED")
    }

    /**
     * Something failed mid-apply, so the outcome is uncertain: retry with backoff (the retry runs find() first),
     * and after maxAttempts hand the intent to a human as NEEDS_ATTENTION.
     */
    @Transactional
    fun fail(claim: Claim, error: String, maxAttempts: Int) {
        val job = lockIfStillOwned(claim) ?: throw LostLeaseException(claim)
        job.lastError = error.take(2000)
        job.leaseOwner = null
        job.leaseUntil = null

        if (job.attempts >= maxAttempts) {
            job.status = JobStatus.DEAD
            intents.findById(claim.intentId).orElseThrow().moveTo(IntentState.NEEDS_ATTENTION)
            finishRun(claim, "NEEDS_ATTENTION")
        } else {
            job.status = JobStatus.PENDING
            // ponytail: plain exponential backoff, capped at 5 min; add jitter if many workers retry in lockstep
            job.availableAt = Instant.now().plusSeconds(minOf(300L, 1L shl job.attempts))
            finishRun(claim, "RETRY")
        }
    }

    // The row lock serialises us against a concurrent claim; the token check rejects a worker that was replaced.
    private fun lockIfStillOwned(claim: Claim): OutboxJob? =
        jobs.lockById(claim.jobId).takeIf { it.status == JobStatus.LEASED && it.fencingToken == claim.fencingToken }

    private fun finishRun(claim: Claim, outcome: String) {
        val run = runs.findByJobIdAndFencingToken(claim.jobId, claim.fencingToken) ?: return
        run.finishedAt = Instant.now()
        run.outcome = outcome
    }
}
