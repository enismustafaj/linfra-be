package ai.linfra.resource_app.policy.rest.dto

import java.math.BigDecimal

/** The tenant's effective policy, its committed spend, and the catalog values it can allow. */
data class PolicyResponse(
    val allowedInstanceTypes: List<String>,
    val allowedRegions: List<String>,
    val monthlyBudgetEur: BigDecimal,
    val autoApproveMaxEur: BigDecimal,
    val requireApprovalForProd: Boolean,
    /** True while the tenant has no policy of its own and the configured defaults apply. */
    val isDefault: Boolean,
    /** Running resources plus approved-but-not-created intents, per month. */
    val committedSpendEur: BigDecimal,
    val catalogInstanceTypes: List<String>,
    val catalogRegions: List<String>,
)
