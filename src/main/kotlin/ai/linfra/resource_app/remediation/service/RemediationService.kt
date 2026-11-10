package ai.linfra.resource_app.remediation.service

import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.intent.persistence.IntentRepository
import ai.linfra.resource_app.outbox.model.JobStatus
import ai.linfra.resource_app.outbox.persistence.OutboxJobRepository
import ai.linfra.resource_app.provider.model.ProvisionedResource
import ai.linfra.resource_app.provider.service.ResourceFinder
import ai.linfra.resource_app.provider.service.ResourceRemover
import ai.linfra.resource_app.remediation.exception.ActionNotAllowedException
import ai.linfra.resource_app.remediation.exception.FindingNotFoundException
import ai.linfra.resource_app.remediation.model.FindingStatus
import ai.linfra.resource_app.remediation.model.RemediationAction
import ai.linfra.resource_app.remediation.model.RemediationAction.ACCEPT
import ai.linfra.resource_app.remediation.model.RemediationAction.ADOPT
import ai.linfra.resource_app.remediation.model.RemediationAction.DESTROY
import ai.linfra.resource_app.remediation.model.RemediationAction.DISMISS
import ai.linfra.resource_app.remediation.model.RemediationAction.KEEP
import ai.linfra.resource_app.remediation.model.RemediationAction.MARK_DESTROYED
import ai.linfra.resource_app.remediation.model.RemediationAction.MARK_NEEDS_ATTENTION
import ai.linfra.resource_app.remediation.model.RemediationAction.RETRY_NOW
import ai.linfra.resource_app.remediation.persistence.Finding
import ai.linfra.resource_app.remediation.persistence.FindingRepository
import ai.linfra.resource_app.resource.model.ResourceStatus
import ai.linfra.resource_app.resource.persistence.Resource
import ai.linfra.resource_app.resource.persistence.ResourceRepository
import ai.linfra.resource_app.terraform.service.TerraformCompiler
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.UUID

/**
 * Applies a user's chosen action to a finding, synchronously, in three steps:
 *   1. check the finding is OPEN and offers the action right now;
 *   2. talk to the provider, outside any transaction (removals are idempotent);
 *   3. record the outcome in one short transaction that fails if the finding changed meanwhile.
 * A crash between 2 and 3 leaves the finding OPEN; the next reconciliation clears it if the problem is gone.
 */
@Service
class RemediationService(
    private val findings: FindingRepository,
    private val options: RemediationOptions,
    private val intents: IntentRepository,
    private val resources: ResourceRepository,
    private val jobs: OutboxJobRepository,
    private val compiler: TerraformCompiler,
    private val finders: List<ResourceFinder>,
    private val removers: List<ResourceRemover>,
    private val tx: TransactionTemplate,
) {

    fun resolve(tenantId: UUID, findingId: UUID, action: RemediationAction, cloudId: String?): Finding {
        val finding = findings.findByIdAndTenantId(findingId, tenantId) ?: throw FindingNotFoundException(findingId)
        val offered = options.actionsFor(finding)
        if (action !in offered) throw ActionNotAllowedException("$action is not available for this finding; available: $offered")

        val adopted = providerStep(finding, action, cloudId)

        return tx.execute {
            val fresh = findings.findById(findingId).orElseThrow()
            if (fresh.version != finding.version || fresh.status != FindingStatus.OPEN) {
                throw ActionNotAllowedException("finding changed while acting on it; reload and try again")
            }
            fresh.resolutionDetail = databaseStep(fresh, action, cloudId, adopted)
            fresh.status = FindingStatus.RESOLVED
            fresh.resolution = action
            fresh.resolvedAt = Instant.now()
            fresh
        }!!
    }

    /** Provider calls only; nothing in the database changes here. */
    private fun providerStep(f: Finding, action: RemediationAction, cloudId: String?): ProvisionedResource? = when (action) {
        DESTROY -> { f.cloudIds.forEach { remover(f).remove(it) }; null }
        KEEP -> {
            if (cloudId == null || cloudId !in f.cloudIds) throw ActionNotAllowedException("KEEP needs a cloudId from ${f.cloudIds}")
            (f.cloudIds - cloudId).forEach { remover(f).remove(it) }
            null
        }
        ADOPT -> {
            val intent = intents.findById(f.intentId!!).orElseThrow()
            val target = compiler.target(intent.id, intent.tenantId, intent.spec)
            finders.firstOrNull { it.provider == target.entry.provider }?.find(target) ?: throw ActionNotAllowedException("the resource no longer exists")
        }
        else -> null
    }

    /** Runs inside the recording transaction; returns a human-readable summary of what was done. */
    private fun databaseStep(f: Finding, action: RemediationAction, cloudId: String?, adopted: ProvisionedResource?): String = when (action) {
        DESTROY -> "removed ${f.cloudIds.joinToString()}"
        KEEP -> {
            resources.findByIntentId(f.intentId!!)?.takeIf { it.cloudId != cloudId }?.let { it.cloudId = cloudId!!; it.updatedAt = Instant.now() }
            "kept $cloudId, removed ${(f.cloudIds - cloudId!!).joinToString()}"
        }
        ADOPT -> {
            val intent = intents.findById(f.intentId!!).orElseThrow()
            if (resources.findByIntentId(intent.id) != null) throw ActionNotAllowedException("intent already has a resource")
            resources.save(
                Resource(
                    tenantId = intent.tenantId, intentId = intent.id, provider = adopted!!.provider, region = adopted.region,
                    instanceType = adopted.instanceType, cloudId = adopted.cloudId, outputs = adopted.outputs,
                )
            )
            intent.moveTo(IntentState.SUCCEEDED)
            "adopted ${adopted.cloudId} as the resource of intent ${intent.id}"
        }
        MARK_DESTROYED -> {
            resources.findById(f.resourceId!!).orElseThrow().apply { status = ResourceStatus.DESTROYED; updatedAt = Instant.now() }
            "resource marked DESTROYED"
        }
        RETRY_NOW -> {
            val job = idleJob(f)
            job.status = JobStatus.PENDING
            job.availableAt = Instant.now()
            job.leaseOwner = null
            job.leaseUntil = null
            "job made claimable now"
        }
        MARK_NEEDS_ATTENTION -> {
            idleJob(f).status = JobStatus.DEAD
            intents.findById(f.intentId!!).orElseThrow().moveTo(IntentState.NEEDS_ATTENTION)
            "intent handed to a human; no more retries"
        }
        DISMISS -> "dismissed; not reported again"
        ACCEPT -> "accepted; this drift is not reported again"
    }

    /** Locks the intent's job and refuses if a worker picked it up since the finding was raised. */
    private fun idleJob(f: Finding) = jobs.lockById(jobs.findByIntentId(f.intentId!!)!!.id!!).also {
        if (it.status == JobStatus.LEASED && it.leaseUntil?.isAfter(Instant.now()) == true) {
            throw ActionNotAllowedException("a worker is working on this intent again")
        }
    }

    private fun remover(f: Finding) = removers.firstOrNull { it.provider == f.provider } ?: throw ActionNotAllowedException("no remover for ${f.provider}")
}
