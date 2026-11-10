package ai.linfra.resource_app.provider.model

import ai.linfra.resource_app.ranker.model.Provider
import java.util.UUID

/**
 * A Linfra-labeled resource as the provider reports it, normalised into catalog terms so the reconciler can compare
 * any provider against what was applied. Null means the provider did not report that attribute.
 */
data class ObservedResource(
    val provider: Provider,
    val cloudId: String,
    /** Null when the label is missing or not a UUID: such a resource can't be traced to an intent. */
    val intentId: UUID?,
    val tenantId: UUID?,
    val instanceType: String?,
    val region: String?,
    val vcpu: Int?,
    val memoryGb: Int?,
    val dedicatedCpu: Boolean?,
)
