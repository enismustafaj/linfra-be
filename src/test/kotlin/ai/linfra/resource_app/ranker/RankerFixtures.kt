package ai.linfra.resource_app.ranker

import ai.linfra.resource_app.conversation.AgentReplies
import ai.linfra.resource_app.conversation.model.ArchitectureTrait
import ai.linfra.resource_app.conversation.model.CpuProfile
import ai.linfra.resource_app.conversation.model.CpuProfileTrait
import ai.linfra.resource_app.conversation.model.DiskTrait
import ai.linfra.resource_app.conversation.model.StorageTrait
import ai.linfra.resource_app.conversation.model.TraitSource
import ai.linfra.resource_app.ranker.model.Architecture
import ai.linfra.resource_app.ranker.model.StorageKind
import ai.linfra.resource_app.ranker.service.CostCalculator
import ai.linfra.resource_app.ranker.service.PriceCatalog
import ai.linfra.resource_app.ranker.service.Ranker
import ai.linfra.resource_app.ranker.service.TraitMatcher

object RankerFixtures {
    val ranker = Ranker(PriceCatalog(), TraitMatcher(), CostCalculator())

    /** 2 vCPU / 4 GB, EU, 10 GB egress, no traits. */
    val base = AgentReplies.smallEuStaging

    fun sustained(source: TraitSource) = CpuProfileTrait(CpuProfile.SUSTAINED, source, "CI builds")
    fun arm(source: TraitSource) = ArchitectureTrait(Architecture.ARM, source, "arm64 images")
    fun localNvme(source: TraitSource) = StorageTrait(StorageKind.LOCAL_NVME, source, "self-hosted Postgres")
    fun disk(minGb: Int, source: TraitSource) = DiskTrait(minGb, source, "media library")
}
