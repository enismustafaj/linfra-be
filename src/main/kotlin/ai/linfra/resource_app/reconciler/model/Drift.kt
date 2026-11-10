package ai.linfra.resource_app.reconciler.model

import ai.linfra.resource_app.ranker.model.Provider
import java.util.UUID

/** The resource exists but differs from what was applied, e.g. resized outside Linfra. */
data class Drift(
    val resourceId: UUID,
    val tenantId: UUID,
    val provider: Provider,
    val cloudId: String,
    /** attribute → (expected, actual) */
    val differences: Map<String, Difference>,
)

data class Difference(val expected: String, val actual: String)
