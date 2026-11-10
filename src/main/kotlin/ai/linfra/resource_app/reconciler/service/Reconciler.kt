package ai.linfra.resource_app.reconciler.service

import ai.linfra.resource_app.provider.service.ResourceFinder
import ai.linfra.resource_app.ranker.model.Provider
import ai.linfra.resource_app.ranker.service.PriceCatalog
import ai.linfra.resource_app.reconciler.model.ReconciliationReport
import ai.linfra.resource_app.reconciler.persistence.InFlightIntentRepository
import ai.linfra.resource_app.resource.model.ResourceStatus
import ai.linfra.resource_app.resource.persistence.ResourceRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

/**
 * Report-only: compares every provider against Linfra's records and reports what differs. The only writes are
 * last_seen_at and ACTIVE/MISSING. It never adopts (the worker's find() does that), deletes or reverts anything.
 */
@Service
class Reconciler(
    private val finders: List<ResourceFinder>,
    private val resources: ResourceRepository,
    private val inFlightIntents: InFlightIntentRepository,
    private val catalog: PriceCatalog,
    private val comparer: InventoryComparer,
    private val tx: TransactionTemplate,
    @Value("\${linfra.reconciler.stuck-after}") private val stuckAfter: Duration,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val latest = AtomicReference<ReconciliationReport?>()

    fun latest(): ReconciliationReport? = latest.get()

    fun reconcile(): ReconciliationReport {
        val now = Instant.now()
        // Our records first, then the provider. A resource a worker creates in between belongs to an intent that is
        // still in flight in this snapshot, so it can't be misreported as an orphan.
        val recorded = resources.findAllByStatusIn(listOf(ResourceStatus.ACTIVE, ResourceStatus.MISSING))
        val inFlight = inFlightIntents.findInFlight(now)
        val observed = finders.flatMap { it.listManaged() }

        val report = comparer.compare(observed, recorded, inFlight, ::catalogEntry, now, stuckAfter)
        record(report)
        latest.set(report)
        logFindings(report)
        return report
    }

    private fun record(report: ReconciliationReport) = tx.executeWithoutResult {
        resources.findAllById(report.seen).forEach {
            it.lastSeenAt = report.checkedAt
            it.status = ResourceStatus.ACTIVE
        }
        resources.findAllById(report.missing.map { it.resourceId }).forEach {
            if (it.status != ResourceStatus.MISSING) it.updatedAt = report.checkedAt
            it.status = ResourceStatus.MISSING
        }
    }

    private fun catalogEntry(provider: Provider, type: String, region: String) =
        catalog.entries.firstOrNull { it.provider == provider && it.instanceType == type && it.region == region }

    private fun logFindings(r: ReconciliationReport) {
        if (r.missing.isEmpty() && r.drifted.isEmpty() && r.orphans.isEmpty() && r.duplicates.isEmpty() && r.stuck.isEmpty()) return
        log.warn(
            "Reconciliation: {} missing, {} drifted, {} orphans, {} duplicates, {} stuck",
            r.missing.size, r.drifted.size, r.orphans.size, r.duplicates.size, r.stuck.size,
        )
    }
}
