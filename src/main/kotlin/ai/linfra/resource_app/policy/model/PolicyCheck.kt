package ai.linfra.resource_app.policy.model

import ai.linfra.resource_app.conversation.model.Environment
import java.math.BigDecimal

/** What the policy is evaluated against: the planned resource and what the tenant already spends. */
data class PolicyCheck(
    val instanceType: String,
    val region: String,
    val environment: Environment,
    val monthlyCostEur: BigDecimal,
    /** Monthly cost of the tenant's active resources and approved-but-not-yet-created intents. */
    val committedSpendEur: BigDecimal,
)
