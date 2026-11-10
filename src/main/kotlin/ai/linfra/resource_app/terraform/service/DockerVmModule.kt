package ai.linfra.resource_app.terraform.service

import ai.linfra.resource_app.ranker.model.Architecture
import ai.linfra.resource_app.ranker.model.CpuModel
import ai.linfra.resource_app.ranker.model.Provider
import ai.linfra.resource_app.ranker.model.StorageKind
import ai.linfra.resource_app.terraform.model.VmTarget
import org.springframework.stereotype.Component

@Component
class DockerVmModule : VmModule {
    override val provider = Provider.DOCKER
    override val moduleName = "docker-vm"

    override fun variables(target: VmTarget) = mapOf(
        "intent_id" to target.intentId.toString(),
        "tenant_id" to target.tenantId.toString(),
        "instance_type" to TerraformValues.requireSafe(target.entry.instanceType),
        "region" to TerraformValues.requireSafe(target.entry.region),
        "vcpu" to target.entry.vcpu,
        "memory_mb" to target.entry.memoryGb * 1024,
        "dedicated_cpu" to (target.entry.cpuModel == CpuModel.DEDICATED),
        "architecture" to if (target.entry.architecture == Architecture.ARM) "arm64" else "amd64",
        "network_disk_gb" to if (target.entry.storage == StorageKind.NETWORK) target.diskGb else 0,
    )
}
