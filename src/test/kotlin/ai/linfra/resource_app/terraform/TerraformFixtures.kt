package ai.linfra.resource_app.terraform

import ai.linfra.resource_app.conversation.model.Draft
import ai.linfra.resource_app.conversation.model.TraitSource.STATED
import ai.linfra.resource_app.conversation.model.VmRequirements
import ai.linfra.resource_app.intent.persistence.Intent
import ai.linfra.resource_app.ranker.RankerFixtures
import ai.linfra.resource_app.ranker.service.PriceCatalog
import ai.linfra.resource_app.terraform.service.DockerVmModule
import ai.linfra.resource_app.terraform.service.TerraformCompiler
import ai.linfra.resource_app.terraform.service.TerraformModules
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.UUID

object TerraformFixtures {
    val mapper = jacksonObjectMapper()

    val modules = TerraformModules()
    val compiler = TerraformCompiler(PriceCatalog(), modules, listOf(DockerVmModule()), mapper)

    /** Ranks to burst.medium in local-eu: burstable, x86, 20 GB network disk. */
    val burstable = RankerFixtures.base

    /** Ranks to storage.medium: dedicated cores, its own local disk. */
    val dedicatedLocalDisk = RankerFixtures.base.copy(storage = RankerFixtures.localNvme(STATED))

    /** Ranks to burst.medium.arm. */
    val arm = RankerFixtures.base.copy(architecture = RankerFixtures.arm(STATED))

    fun intent(requirements: VmRequirements, edit: (ObjectNode) -> Unit = {}): Intent {
        val draft = Draft(requirements, RankerFixtures.ranker.pick(requirements)!!, "why")
        val spec = mapper.valueToTree<ObjectNode>(draft).also(edit)
        return Intent(tenantId = UUID.randomUUID(), spec = mapper.convertValue(spec))
    }

    fun tfvars(intent: Intent): JsonNode = mapper.readTree(compiler.compile(intent).tfvarsJson)
}
