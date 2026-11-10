package ai.linfra.resource_app.conversation.model

import ai.linfra.resource_app.ranker.model.Architecture
import ai.linfra.resource_app.ranker.model.StorageKind

/** A workload property beyond size, with where it came from and why. */
sealed interface Trait {
    val source: TraitSource
    /** Shown to the user in the draft, e.g. "you mentioned CI builds". */
    val reason: String
}

data class CpuProfileTrait(val value: CpuProfile, override val source: TraitSource, override val reason: String) : Trait

data class ArchitectureTrait(val value: Architecture, override val source: TraitSource, override val reason: String) : Trait

data class StorageTrait(val value: StorageKind, override val source: TraitSource, override val reason: String) : Trait

data class DiskTrait(val minGb: Int, override val source: TraitSource, override val reason: String) : Trait
