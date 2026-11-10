package ai.linfra.resource_app.reconciler

import ai.linfra.resource_app.IntegrationTest
import ai.linfra.resource_app.provider.model.ObservedResource
import ai.linfra.resource_app.ranker.model.Provider
import ai.linfra.resource_app.reconciler.service.Reconciler
import ai.linfra.resource_app.resource.model.ResourceStatus
import ai.linfra.resource_app.resource.persistence.Resource
import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ReconcilerTest : IntegrationTest() {

    @Autowired lateinit var reconciler: Reconciler
    @Autowired lateinit var meters: MeterRegistry

    private fun resource(key: String): Resource {
        val intent = plannedIntent(tenant(key).id)
        return resources.save(
            Resource(
                tenantId = intent.tenantId, intentId = intent.id, provider = Provider.DOCKER, region = "local-eu",
                instanceType = "burst.medium", cloudId = "c-${UUID.randomUUID()}", outputs = emptyMap(),
            )
        )
    }

    private fun present(r: Resource) = ObservedResource(
        Provider.DOCKER, r.cloudId, r.intentId, r.tenantId, "burst.medium", "local-eu", 2, 4, false,
    )

    @Test
    fun `missing resources are marked MISSING and come back ACTIVE when seen again`() {
        val resource = resource("k-${UUID.randomUUID()}")

        whenever(finder.listManaged()) doReturn emptyList()
        reconciler.reconcile()
        assertEquals(ResourceStatus.MISSING, resources.findById(resource.id).get().status)

        whenever(finder.listManaged()) doReturn listOf(present(resource))
        reconciler.reconcile()
        val seen = resources.findById(resource.id).get()
        assertEquals(ResourceStatus.ACTIVE, seen.status)
        assertNotNull(seen.lastSeenAt)
    }

    @Test
    fun `metrics expose the latest counts`() {
        whenever(finder.listManaged()) doReturn listOf(present(resource("k-${UUID.randomUUID()}")).copy(cloudId = "unrecorded", intentId = null))

        reconciler.reconcile()

        assertEquals(1.0, meters.get("linfra.reconciler.orphans").gauge().value())
        assertEquals(1.0, meters.get("linfra.reconciler.missing").gauge().value())
    }
}
