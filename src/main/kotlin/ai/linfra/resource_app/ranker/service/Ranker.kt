package ai.linfra.resource_app.ranker.service

import ai.linfra.resource_app.conversation.model.TraitSource
import ai.linfra.resource_app.conversation.model.VmRequirements
import ai.linfra.resource_app.ranker.model.Architecture
import ai.linfra.resource_app.ranker.model.Area
import ai.linfra.resource_app.ranker.model.RankedPick
import org.springframework.stereotype.Service

/**
 * Deterministic: the LLM extracts requirements, this picks. Lexicographic, so the explanation can state the rule:
 *   1. drop entries failing size, area, budget or any STATED trait
 *   2. prefer entries meeting the most INFERRED traits
 *   3. then the lowest total monthly cost
 */
@Service
class Ranker(
    private val catalog: PriceCatalog,
    private val traits: TraitMatcher,
    private val costs: CostCalculator,
) {
    fun pick(req: VmRequirements): RankedPick? {
        val stated = req.traits().filter { it.source == TraitSource.STATED }
        val inferred = req.traits().filter { it.source == TraitSource.INFERRED }

        return catalog.entries
            .filter { it.vcpu >= req.vcpuMin && it.memoryGb >= req.memoryGbMin }
            .filter { req.area == Area.ANY || it.area == req.area }
            // No architecture mentioned: assume x86 images. Offering ARM silently would break most deployments.
            .filter { req.architecture != null || it.architecture == Architecture.X86 }
            .filter { entry -> stated.all { traits.satisfies(it, entry) } }
            .map { entry ->
                val (met, missed) = inferred.partition { traits.satisfies(it, entry) }
                RankedPick(entry, costs.cost(entry, req), costs.diskGb(entry, req), met.map(traits::label), missed.map(traits::label))
            }
            .filter { req.maxMonthlyEur == null || it.cost.total <= req.maxMonthlyEur }
            .minWithOrNull(compareByDescending<RankedPick> { it.preferencesMet.size }.thenBy { it.cost.total })
    }
}
