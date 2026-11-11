package ai.linfra.resource_app.policy.rest

import ai.linfra.resource_app.policy.model.PolicyUpdate
import ai.linfra.resource_app.policy.rest.dto.PolicyResponse
import ai.linfra.resource_app.policy.service.PolicyService
import ai.linfra.resource_app.ranker.service.PriceCatalog
import ai.linfra.resource_app.tenant.model.TenantContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/** The caller's own tenant policy. */
// ponytail: any API key of the tenant may change its policy; add roles when tenants have more than one user
@RestController
@RequestMapping("/api/v1/policy")
class PolicyController(
    private val policies: PolicyService,
    private val catalog: PriceCatalog,
) {
    @GetMapping
    fun get(tenant: TenantContext) = response(tenant.tenantId)

    @PutMapping
    fun update(tenant: TenantContext, @RequestBody body: PolicyUpdate): PolicyResponse {
        policies.update(tenant.tenantId, body)
        return response(tenant.tenantId)
    }

    private fun response(tenantId: UUID): PolicyResponse {
        val policy = policies.policyFor(tenantId)
        return PolicyResponse(
            allowedInstanceTypes = policy.allowedInstanceTypes,
            allowedRegions = policy.allowedRegions,
            monthlyBudgetEur = policy.monthlyBudgetEur,
            autoApproveMaxEur = policy.autoApproveMaxEur,
            requireApprovalForProd = policy.requireApprovalForProd,
            isDefault = !policies.hasOwnPolicy(tenantId),
            committedSpendEur = policies.committedSpend(tenantId),
            catalogInstanceTypes = catalog.entries.map { it.instanceType }.distinct().sorted(),
            catalogRegions = catalog.entries.map { it.region }.distinct().sorted(),
        )
    }
}
