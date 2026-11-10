package ai.linfra.resource_app.reconciler.service

import ai.linfra.resource_app.provider.model.ObservedResource
import ai.linfra.resource_app.ranker.model.CatalogEntry
import ai.linfra.resource_app.ranker.model.CpuModel
import ai.linfra.resource_app.ranker.model.Provider
import ai.linfra.resource_app.reconciler.model.Difference
import ai.linfra.resource_app.reconciler.model.Drift
import ai.linfra.resource_app.reconciler.model.Duplicate
import ai.linfra.resource_app.reconciler.model.InFlightIntent
import ai.linfra.resource_app.reconciler.model.MissingResource
import ai.linfra.resource_app.reconciler.model.Orphan
import ai.linfra.resource_app.reconciler.model.ReconciliationReport
import ai.linfra.resource_app.reconciler.model.StuckIntent
import ai.linfra.resource_app.resource.persistence.Resource
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * Pure comparison of what the provider has against what Linfra recorded. No I/O, so it is unit-tested exhaustively.
 */
@Component
class InventoryComparer {

    fun compare(
        observed: List<ObservedResource>,
        recorded: List<Resource>,
        inFlight: List<InFlightIntent>,
        catalog: (Provider, String, String) -> CatalogEntry?,
        now: Instant,
        stuckAfter: Duration,
    ): ReconciliationReport {
        val observedById = observed.associateBy { it.cloudId }
        val recordedIds = recorded.map { it.cloudId }.toSet()
        val inFlightIds = inFlight.map { it.intentId }.toSet()

        val (present, absent) = recorded.partition { it.cloudId in observedById }

        return ReconciliationReport(
            checkedAt = now,
            seen = present.map { it.id },
            missing = absent.map { MissingResource(it.id, it.tenantId, it.provider, it.cloudId) },
            drifted = present.mapNotNull { drift(it, observedById.getValue(it.cloudId), catalog) },
            orphans = observed
                .filter { it.cloudId !in recordedIds && it.intentId !in inFlightIds }
                .map { Orphan(it.provider, it.cloudId, it.intentId, it.tenantId, if (it.intentId == null) "no intent_id label" else "no resource recorded and intent not in progress") },
            duplicates = observed.filter { it.intentId != null }.groupBy { it.intentId!! }
                .filterValues { it.size > 1 }
                .map { (intentId, copies) -> Duplicate(intentId, copies.first().tenantId, copies.first().provider, copies.map { it.cloudId }.sorted()) },
            stuck = inFlight
                .filter { it.liveLeaseUntil == null && Duration.between(it.dueSince, now) > stuckAfter }
                .map { StuckIntent(it.intentId, it.tenantId, it.state, Duration.between(it.dueSince, now).seconds) },
            oldestUnfinishedIntentAgeSeconds = inFlight.maxOfOrNull { Duration.between(it.approvedAt, now).seconds },
        )
    }

    private fun drift(resource: Resource, actual: ObservedResource, catalog: (Provider, String, String) -> CatalogEntry?): Drift? {
        val entry = catalog(resource.provider, resource.instanceType, resource.region)
        val differences = buildMap {
            fun check(name: String, expected: Any?, observed: Any?) {
                if (expected != null && observed != null && expected != observed) put(name, Difference(expected.toString(), observed.toString()))
            }
            check("instance_type", resource.instanceType, actual.instanceType)
            check("region", resource.region, actual.region)
            check("vcpu", entry?.vcpu, actual.vcpu)
            check("memory_gb", entry?.memoryGb, actual.memoryGb)
            check("dedicated_cpu", entry?.let { it.cpuModel == CpuModel.DEDICATED }, actual.dedicatedCpu)
        }
        return if (differences.isEmpty()) null else Drift(resource.id, resource.tenantId, resource.provider, resource.cloudId, differences)
    }
}
