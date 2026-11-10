package ai.linfra.resource_app.provider.service

import ai.linfra.resource_app.provider.exception.DuplicateResourceException
import ai.linfra.resource_app.provider.model.ObservedResource
import ai.linfra.resource_app.provider.model.ProvisionedResource
import ai.linfra.resource_app.ranker.model.Provider
import ai.linfra.resource_app.terraform.model.VmTarget
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val GIB = 1024L * 1024 * 1024

/** Finds containers through the Docker CLI by label, including stopped ones: a stopped container still exists. */
@Component
class DockerContainerFinder(private val objectMapper: ObjectMapper) : ResourceFinder {
    override val provider = Provider.DOCKER

    override fun find(target: VmTarget): ProvisionedResource? {
        val ids = managedIds("label=intent_id=${target.intentId}")
        if (ids.size > 1) throw DuplicateResourceException(target.intentId, ids)
        val container = inspect(ids).singleOrNull() ?: return null

        return ProvisionedResource(
            provider = Provider.DOCKER,
            region = target.entry.region,
            instanceType = target.entry.instanceType,
            cloudId = container["Id"].asText(),
            outputs = mapOf("ipv4" to ipv4(container)),
        )
    }

    override fun listManaged(): List<ObservedResource> = inspect(managedIds()).map { c ->
        val labels = c["Config"]["Labels"]
        val host = c["HostConfig"]
        val cpuSet = host["CpusetCpus"]?.asText().orEmpty()
        ObservedResource(
            provider = Provider.DOCKER,
            cloudId = c["Id"].asText(),
            intentId = labels["intent_id"]?.asText()?.let { runCatching { UUID.fromString(it) }.getOrNull() },
            tenantId = labels["tenant_id"]?.asText()?.let { runCatching { UUID.fromString(it) }.getOrNull() },
            instanceType = labels["instance_type"]?.asText(),
            region = labels["region"]?.asText(),
            // Same mapping the docker-vm module applies: pinned cores when dedicated, CPU shares (512 per vCPU) otherwise.
            vcpu = if (cpuSet.isNotEmpty()) cpuCount(cpuSet) else host["CpuShares"]?.asInt()?.takeIf { it > 0 }?.div(512),
            memoryGb = host["Memory"]?.asLong()?.takeIf { it > 0 }?.let { (it / GIB).toInt() },
            dedicatedCpu = cpuSet.isNotEmpty(),
        )
    }

    private fun managedIds(vararg filters: String): List<String> =
        docker("ps", "--all", "--no-trunc", "--format", "{{.ID}}", "--filter", "label=managed_by=linfra",
            *filters.flatMap { listOf("--filter", it) }.toTypedArray())
            .lines().filter { it.isNotBlank() }

    private fun inspect(ids: List<String>): List<JsonNode> =
        if (ids.isEmpty()) emptyList() else objectMapper.readTree(docker("inspect", *ids.toTypedArray())).toList()

    private fun ipv4(container: JsonNode): String? =
        container["NetworkSettings"]["Networks"]?.firstOrNull()?.get("IPAddress")?.asText()?.ifEmpty { null }

    /** "0-1,3" → 3 */
    private fun cpuCount(cpuSet: String): Int = cpuSet.split(",").sumOf { part ->
        val bounds = part.split("-").map { it.trim().toInt() }
        if (bounds.size == 2) bounds[1] - bounds[0] + 1 else 1
    }

    private fun docker(vararg args: String): String {
        val process = ProcessBuilder("docker", *args).start()
        val output = process.inputStream.bufferedReader().readText()
        val errors = process.errorStream.bufferedReader().readText()
        check(process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0) { "docker ${args.first()} failed: $errors" }
        return output
    }
}
