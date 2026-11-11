package ai.linfra.resource_app.policy.model

import java.math.BigDecimal

/** A tenant's new policy, as submitted. Empty lists mean "any catalog value". */
data class PolicyUpdate(
    val allowedInstanceTypes: List<String>,
    val allowedRegions: List<String>,
    val monthlyBudgetEur: BigDecimal,
    val autoApproveMaxEur: BigDecimal,
    val requireApprovalForProd: Boolean,
)
