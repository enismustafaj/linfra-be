package ai.linfra.resource_app.terraform.service

import ai.linfra.resource_app.provider.model.ProvisionedResource
import ai.linfra.resource_app.ranker.model.Provider
import ai.linfra.resource_app.terraform.model.VmTarget

/**
 * Maps a target to the input variables of a checked-in module under resources/terraform/<moduleName>.
 * Adding a resource type = adding a module folder and one implementation of this.
 */
interface VmModule {
    val provider: Provider
    val moduleName: String
    fun variables(target: VmTarget): Map<String, Any>

    /**
     * The module's resources as OpenTofu import arguments (address → provider id) for a resource found by intent_id,
     * so adopting it can bring it into the intent's state.
     */
    fun imports(target: VmTarget, found: ProvisionedResource): Map<String, String>
}
