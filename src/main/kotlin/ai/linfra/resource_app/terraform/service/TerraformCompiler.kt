package ai.linfra.resource_app.terraform.service

import ai.linfra.resource_app.conversation.model.Draft
import ai.linfra.resource_app.intent.persistence.Intent
import ai.linfra.resource_app.provider.model.ProvisionedResource
import ai.linfra.resource_app.ranker.service.PriceCatalog
import ai.linfra.resource_app.terraform.exception.NotInAllowListException
import ai.linfra.resource_app.terraform.model.CompiledWorkspace
import ai.linfra.resource_app.terraform.model.VmTarget
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID

const val TFVARS_FILE = "terraform.tfvars.json"

/**
 * Intent → checked-in module + generated terraform.tfvars.json. Linfra never generates Terraform code, only values,
 * and only allow-listed ones: the catalog entry is looked up by key and the catalog's own copy is used, so nothing
 * the LLM or a client wrote reaches the workspace.
 */
@Service
class TerraformCompiler(
    private val catalog: PriceCatalog,
    private val modules: TerraformModules,
    vmModules: List<VmModule>,
    objectMapper: ObjectMapper,
) {
    private val vmModules = vmModules.associateBy { it.provider }
    private val canonicalJson = objectMapper.copy()
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .enable(SerializationFeature.INDENT_OUTPUT)

    fun compile(intent: Intent): CompiledWorkspace = compile(target(intent.id, intent.tenantId, intent.spec))

    /** Resolves an intent's spec to an allow-listed target. Also used by find(), so it searches for exactly what apply creates. */
    fun target(intentId: UUID, tenantId: UUID, spec: Map<String, Any?>): VmTarget {
        val draft = canonicalJson.convertValue(spec, Draft::class.java)
        val requested = draft.pick.entry

        val entry = catalog.entries.firstOrNull {
            it.provider == requested.provider && it.instanceType == requested.instanceType && it.region == requested.region
        } ?: throw NotInAllowListException("${requested.provider}/${requested.instanceType}/${requested.region} is not in the catalog")
        if (draft.pick.diskGb !in 10..2000) throw NotInAllowListException("diskGb ${draft.pick.diskGb} out of range")

        return VmTarget(intentId, tenantId, entry, draft.pick.diskGb)
    }

    /** Import arguments for adopting [found] into the intent's state. */
    fun imports(target: VmTarget, found: ProvisionedResource): Map<String, String> =
        (vmModules[target.entry.provider] ?: throw NotInAllowListException("no module for ${target.entry.provider}")).imports(target, found)

    fun compile(target: VmTarget): CompiledWorkspace {
        val vmModule = vmModules[target.entry.provider] ?: throw NotInAllowListException("no module for ${target.entry.provider}")
        val variables = vmModule.variables(target)

        // Kotlin and HCL must agree exactly: a missing variable would fail at plan, an extra one would be ignored silently.
        val declared = modules.declaredVariables(vmModule.moduleName)
        check(variables.keys == declared) { "Module ${vmModule.moduleName} declares $declared but got ${variables.keys}" }

        val files = modules.files(vmModule.moduleName)
        val tfvars = canonicalJson.writeValueAsString(variables)
        return CompiledWorkspace(vmModule.moduleName, files, tfvars, hash(files + (TFVARS_FILE to tfvars)))
    }

    private fun hash(files: Map<String, String>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        files.toSortedMap().forEach { (name, content) -> digest.update("$name\n$content\n".toByteArray()) }
        return HexFormat.of().formatHex(digest.digest())
    }
}
