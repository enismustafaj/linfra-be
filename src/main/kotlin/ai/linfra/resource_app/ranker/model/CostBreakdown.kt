package ai.linfra.resource_app.ranker.model

import java.math.BigDecimal

/** Monthly EUR. Comparing instance list prices alone is misleading: egress and IPv4 differ a lot between providers. */
data class CostBreakdown(
    val instance: BigDecimal,
    val disk: BigDecimal,
    val egress: BigDecimal,
    val ipv4: BigDecimal,
    val total: BigDecimal,
)
