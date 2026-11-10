package ai.linfra.resource_app.provider.model

import ai.linfra.resource_app.ranker.model.Provider

/** What an adapter reports after find() or apply(): what it applied and what the cloud returned. */
data class ProvisionedResource(
    val provider: Provider,
    val region: String,
    val instanceType: String,
    val cloudId: String,
    val outputs: Map<String, Any?>,
)
