package ai.linfra.resource_app.provider.service

import ai.linfra.resource_app.provider.model.ObservedResource
import ai.linfra.resource_app.provider.model.ProvisionedResource
import ai.linfra.resource_app.ranker.model.Provider
import ai.linfra.resource_app.terraform.model.VmTarget

interface ResourceFinder {
    val provider: Provider

    fun find(target: VmTarget): ProvisionedResource?

    fun listManaged(): List<ObservedResource>
}
