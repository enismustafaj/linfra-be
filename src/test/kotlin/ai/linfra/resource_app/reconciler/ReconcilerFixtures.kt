package ai.linfra.resource_app.reconciler

import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.provider.model.ObservedResource
import ai.linfra.resource_app.ranker.model.Provider
import ai.linfra.resource_app.ranker.service.PriceCatalog
import ai.linfra.resource_app.reconciler.model.InFlightIntent
import ai.linfra.resource_app.reconciler.model.ReconciliationReport
import ai.linfra.resource_app.reconciler.service.InventoryComparer
import ai.linfra.resource_app.resource.persistence.Resource
import java.time.Duration
import java.time.Instant
import java.util.UUID

object ReconcilerFixtures {
    val now: Instant = Instant.parse("2026-11-10T12:00:00Z")
    val tenantId: UUID = UUID.randomUUID()
    private val catalog = PriceCatalog()

    /** A recorded burst.medium in local-eu. */
    fun recorded(cloudId: String = "c-${UUID.randomUUID()}", intentId: UUID = UUID.randomUUID()) = Resource(
        tenantId = tenantId, intentId = intentId, provider = Provider.DOCKER, region = "local-eu",
        instanceType = "burst.medium", cloudId = cloudId, outputs = emptyMap(),
    )

    /** What Docker reports for an untouched burst.medium. */
    fun observed(cloudId: String, intentId: UUID? = UUID.randomUUID()) = ObservedResource(
        provider = Provider.DOCKER, cloudId = cloudId, intentId = intentId, tenantId = tenantId,
        instanceType = "burst.medium", region = "local-eu", vcpu = 2, memoryGb = 4, dedicatedCpu = false,
    )

    fun inFlight(intentId: UUID, dueAgo: Duration, liveLease: Boolean = false) = InFlightIntent(
        intentId = intentId, tenantId = tenantId, state = IntentState.APPLYING,
        approvedAt = now.minus(dueAgo).minusSeconds(60), dueSince = now.minus(dueAgo),
        liveLeaseUntil = if (liveLease) now.plusSeconds(10) else null,
    )

    fun compare(
        observed: List<ObservedResource> = emptyList(),
        recorded: List<Resource> = emptyList(),
        inFlight: List<InFlightIntent> = emptyList(),
    ): ReconciliationReport = InventoryComparer().compare(
        observed, recorded, inFlight,
        { p, t, r -> catalog.entries.firstOrNull { it.provider == p && it.instanceType == t && it.region == r } },
        now, Duration.ofMinutes(2),
    )
}
