package ai.linfra.resource_app.provider.service

import ai.linfra.resource_app.outbox.model.Claim
import ai.linfra.resource_app.provider.exception.PlanChangedException
import ai.linfra.resource_app.provider.model.ProvisionedResource
import ai.linfra.resource_app.terraform.service.TerraformCompiler
import ai.linfra.resource_app.terraform.service.TofuRunner
import org.springframework.stereotype.Service
import java.util.UUID

/** What the worker calls: find() asks the provider directly, apply() compiles the intent and runs OpenTofu. */
@Service
class Provisioner(
    private val compiler: TerraformCompiler,
    private val tofu: TofuRunner,
    finders: List<ResourceFinder>,
) {
    private val finders = finders.associateBy { it.provider }

    fun find(claim: Claim): ProvisionedResource? {
        val target = compiler.target(claim.intentId, claim.tenantId, claim.spec)
        val finder = finders[target.entry.provider] ?: error("no finder for ${target.entry.provider}")
        return finder.find(target)
    }

    /**
     * Applies exactly what was approved: the workspace is recompiled and must hash to the approved plan. If the module,
     * catalog or values changed since approval, nothing is applied.
     */
    // ponytail: the plan is the compiled workspace's hash, not a saved `tofu plan` file; add one when plans need review
    fun apply(claim: Claim): ProvisionedResource {
        val target = compiler.target(claim.intentId, claim.tenantId, claim.spec)
        val workspace = compiler.compile(target)
        if (claim.planHash != null && workspace.sha256 != claim.planHash) throw PlanChangedException(claim.intentId)
        val outputs = tofu.apply(workspace, stateName(claim), runId = "${claim.intentId}-${claim.fencingToken}")
        return ProvisionedResource(
            provider = target.entry.provider,
            region = target.entry.region,
            instanceType = target.entry.instanceType,
            cloudId = outputs["resource_id"] as String,
            outputs = mapOf("ipv4" to outputs["ipv4"]),
        )
    }

    /**
     * Brings a resource [find] returned into the intent's OpenTofu state, if it isn't there yet. Needed when a worker
     * died after the provider created the resource but before OpenTofu saved state: without it, the resource would be
     * recorded by Linfra but unknown to OpenTofu, and a later destroy would leave it behind.
     */
    fun adopt(intentId: UUID, tenantId: UUID, spec: Map<String, Any?>, found: ProvisionedResource, runId: String): List<String> {
        val target = compiler.target(intentId, tenantId, spec)
        return tofu.importMissing(compiler.compile(target), "intent-$intentId", runId, compiler.imports(target, found))
    }

    fun adopt(claim: Claim, found: ProvisionedResource): List<String> =
        adopt(claim.intentId, claim.tenantId, claim.spec, found, runId = "${claim.intentId}-${claim.fencingToken}")

    fun destroy(claim: Claim) {
        val target = compiler.target(claim.intentId, claim.tenantId, claim.spec)
        tofu.destroy(compiler.compile(target), stateName(claim), runId = "${claim.intentId}-destroy")
    }

    private fun stateName(claim: Claim) = "intent-${claim.intentId}"
}
