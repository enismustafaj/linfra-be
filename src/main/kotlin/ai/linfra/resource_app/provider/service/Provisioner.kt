package ai.linfra.resource_app.provider.service

import ai.linfra.resource_app.outbox.model.Claim
import ai.linfra.resource_app.provider.model.ProvisionedResource
import ai.linfra.resource_app.terraform.service.TerraformCompiler
import ai.linfra.resource_app.terraform.service.TofuRunner
import org.springframework.stereotype.Service

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

    // ponytail: applies without a saved plan; the planner will produce a plan file whose hash the approval is bound to
    fun apply(claim: Claim): ProvisionedResource {
        val target = compiler.target(claim.intentId, claim.tenantId, claim.spec)
        val outputs = tofu.apply(compiler.compile(target), stateName(claim), runId = "${claim.intentId}-${claim.fencingToken}")
        return ProvisionedResource(
            provider = target.entry.provider,
            region = target.entry.region,
            instanceType = target.entry.instanceType,
            cloudId = outputs["resource_id"] as String,
            outputs = mapOf("ipv4" to outputs["ipv4"]),
        )
    }

    fun destroy(claim: Claim) {
        val target = compiler.target(claim.intentId, claim.tenantId, claim.spec)
        tofu.destroy(compiler.compile(target), stateName(claim), runId = "${claim.intentId}-destroy")
    }

    private fun stateName(claim: Claim) = "intent-${claim.intentId}"
}
