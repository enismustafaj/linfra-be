package ai.linfra.resource_app.provider

import ai.linfra.resource_app.TestcontainersConfiguration
import ai.linfra.resource_app.conversation.model.Draft
import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.intent.persistence.Intent
import ai.linfra.resource_app.intent.persistence.IntentRepository
import ai.linfra.resource_app.intent.service.IntentService
import ai.linfra.resource_app.outbox.model.Claim
import ai.linfra.resource_app.outbox.persistence.OutboxJobRepository
import ai.linfra.resource_app.outbox.service.OutboxWorker
import ai.linfra.resource_app.provider.service.Provisioner
import ai.linfra.resource_app.ranker.RankerFixtures
import ai.linfra.resource_app.resource.model.ResourceStatus
import ai.linfra.resource_app.resource.persistence.ResourceRepository
import ai.linfra.resource_app.tenant.persistence.Tenant
import ai.linfra.resource_app.tenant.persistence.TenantRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

/**
 * End to end on real infrastructure: worker → OpenTofu (pg backend in the test Postgres) → local Docker.
 * Opt-in with `mvn verify -Ptofu`; needs tofu and a Docker daemon. Destroys everything it creates.
 */
@Tag("tofu")
@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["linfra.worker.enabled=false", "linfra.reconciler.enabled=false"])
class DockerProvisioningTest {

    @Autowired lateinit var tenants: TenantRepository
    @Autowired lateinit var intents: IntentRepository
    @Autowired lateinit var intentService: IntentService
    @Autowired lateinit var jobs: OutboxJobRepository
    @Autowired lateinit var resources: ResourceRepository
    @Autowired lateinit var worker: OutboxWorker
    @Autowired lateinit var provisioner: Provisioner
    @Autowired lateinit var objectMapper: ObjectMapper

    private val created = mutableListOf<Claim>()

    private fun approvedIntent(): Intent {
        val tenant = tenants.save(Tenant(name = "t", apiKeyHash = UUID.randomUUID().toString()))
        val draft = Draft(RankerFixtures.base, RankerFixtures.ranker.pick(RankerFixtures.base)!!, "why")
        val intent = intents.save(Intent(tenantId = tenant.id, spec = objectMapper.convertValue(draft), state = IntentState.PLANNED, planHash = "h"))
        intentService.approve(tenant.id, intent.id, "h", Duration.ofMinutes(10))
        return intent.also { created += Claim(0, it.id, it.tenantId, it.spec, 0, 0) }
    }

    private fun containersFor(intentId: UUID): List<String> {
        val p = ProcessBuilder("docker", "ps", "-aq", "--no-trunc", "--filter", "label=intent_id=$intentId").start()
        return p.inputStream.bufferedReader().readText().lines().filter { it.isNotBlank() }.also { p.waitFor(30, TimeUnit.SECONDS) }
    }

    @AfterEach
    fun destroyEverything() {
        created.forEach { provisioner.destroy(it) }
        // What a destroy flow will do: otherwise the reconciler rightly reports these as MISSING.
        created.forEach { c -> resources.findByIntentId(c.intentId)?.let { it.status = ResourceStatus.DESTROYED; resources.save(it) } }
        created.forEach { assertEquals(emptyList(), containersFor(it.intentId), "container left behind for ${it.intentId}") }
    }

    @Test
    fun `worker creates exactly one container and records it`() {
        val intent = approvedIntent()

        worker.processNext("it")

        assertEquals(IntentState.SUCCEEDED, intents.findById(intent.id).get().state)
        val resource = resources.findByIntentId(intent.id)!!
        assertEquals(listOf(resource.cloudId), containersFor(intent.id))
        assertEquals("burst.medium", resource.instanceType)
    }

    @Test
    fun `a container created by a worker that died before recording it is adopted, not duplicated`() {
        val intent = approvedIntent()
        // Worker 1 claims and applies, then "dies" before succeed(): container exists, nothing recorded.
        val job = jobs.findByIntentId(intent.id)!!
        val orphan = provisioner.apply(Claim(job.id!!, intent.id, intent.tenantId, intent.spec, 1, 1))
        job.status = ai.linfra.resource_app.outbox.model.JobStatus.LEASED
        job.leaseUntil = Instant.now().minusSeconds(1)
        jobs.save(job)
        intents.findById(intent.id).get().let { it.moveTo(IntentState.APPLYING); intents.save(it) }

        worker.processNext("it") // worker 2 takes over the expired lease

        assertEquals(IntentState.SUCCEEDED, intents.findById(intent.id).get().state)
        assertEquals(orphan.cloudId, resources.findByIntentId(intent.id)!!.cloudId)
        assertEquals(listOf(orphan.cloudId), containersFor(intent.id))
    }
}
