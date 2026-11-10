package ai.linfra.resource_app.ranker.service

import ai.linfra.resource_app.conversation.model.ArchitectureTrait
import ai.linfra.resource_app.conversation.model.CpuProfile
import ai.linfra.resource_app.conversation.model.CpuProfileTrait
import ai.linfra.resource_app.conversation.model.DiskTrait
import ai.linfra.resource_app.conversation.model.StorageTrait
import ai.linfra.resource_app.conversation.model.Trait
import ai.linfra.resource_app.ranker.model.CatalogEntry
import ai.linfra.resource_app.ranker.model.CpuModel
import ai.linfra.resource_app.ranker.model.StorageKind
import org.springframework.stereotype.Component

/** Whether a catalog entry satisfies a workload trait, and how to name that trait to the user. */
@Component
class TraitMatcher {

    fun satisfies(trait: Trait, entry: CatalogEntry): Boolean = when (trait) {
        is CpuProfileTrait -> trait.value == CpuProfile.BURSTABLE_OK || entry.cpuModel == CpuModel.DEDICATED
        is ArchitectureTrait -> entry.architecture == trait.value
        is StorageTrait -> entry.storage == trait.value
        // Network disks are provisioned at any size; local disks are fixed by the instance type.
        is DiskTrait -> entry.storage == StorageKind.NETWORK || entry.includedDiskGb >= trait.minGb
    }

    fun label(trait: Trait): String = when (trait) {
        is CpuProfileTrait -> if (trait.value == CpuProfile.SUSTAINED) "sustained CPU" else "burstable CPU is fine"
        is ArchitectureTrait -> "${trait.value} architecture"
        is StorageTrait -> if (trait.value == StorageKind.LOCAL_NVME) "local NVMe disk" else "network disk"
        is DiskTrait -> "at least ${trait.minGb} GB disk"
    }
}
