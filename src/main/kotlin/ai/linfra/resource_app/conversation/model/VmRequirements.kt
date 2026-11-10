package ai.linfra.resource_app.conversation.model

import ai.linfra.resource_app.ranker.model.Area
import java.math.BigDecimal

/**
 * What the LLM may extract. No provider, instance type, name, tags or credentials: those are decided by code.
 * Size, area and budget are always hard constraints; the optional traits carry their own STATED/INFERRED source.
 */
data class VmRequirements(
    val vcpuMin: Int,
    val memoryGbMin: Int,
    val area: Area,
    val environment: Environment,
    val maxMonthlyEur: BigDecimal?,
    /** Rough outbound traffic estimate; drives total cost, never filters. 0 when unknown or negligible. */
    val estimatedEgressGbPerMonth: Int,
    val cpuProfile: CpuProfileTrait?,
    val architecture: ArchitectureTrait?,
    val storage: StorageTrait?,
    val disk: DiskTrait?,
    /** Free text for display only; never compiled into infrastructure. */
    val purpose: String,
) {
    fun traits(): List<Trait> = listOfNotNull(cpuProfile, architecture, storage, disk)

    fun validationErrors(): List<String> = buildList {
        if (vcpuMin !in 1..16) add("vcpuMin must be 1..16")
        if (memoryGbMin !in 1..64) add("memoryGbMin must be 1..64")
        if (maxMonthlyEur != null && maxMonthlyEur <= BigDecimal.ZERO) add("maxMonthlyEur must be positive")
        if (estimatedEgressGbPerMonth !in 0..1_000_000) add("estimatedEgressGbPerMonth must be 0..1000000")
        if (disk != null && disk.minGb !in 10..2000) add("disk.minGb must be 10..2000")
        if (purpose.length > 200) add("purpose must be at most 200 characters")
        if (traits().any { it.reason.length > 200 }) add("trait reasons must be at most 200 characters")
    }
}
