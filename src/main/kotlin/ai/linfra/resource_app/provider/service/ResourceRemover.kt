package ai.linfra.resource_app.provider.service

import ai.linfra.resource_app.ranker.model.Provider

/** Removes a resource from the provider by its cloud id. Idempotent: removing something already gone succeeds. */
interface ResourceRemover {
    val provider: Provider
    fun remove(cloudId: String)
}
