package ai.linfra.resource_app.provider

import ai.linfra.resource_app.TestcontainersConfiguration
import ai.linfra.resource_app.conversation.model.Draft
import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.intent.persistence.Intent
import ai.linfra.resource_app.intent.persistence.IntentRepository
import ai.linfra.resource_app.intent.service.IntentPlanner
import ai.linfra.resource_app.intent.service.IntentService
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
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
    @Autowired lateinit var planner: IntentPlanner
    @Autowired lateinit var tx: TransactionTemplate
    @Autowired lateinit var jdbc: JdbcTemplate

    private val created = mutableListOf<Claim>()

    private fun approvedIntent(): Intent {
        val tenant = tenants.save(Tenant(name = "t", apiKeyHash = UUID.randomUUID().toString()))
        val draft = Draft(RankerFixtures.base, RankerFixtures.ranker.pick(RankerFixtures.base)!!, "why")
        // The real path: plan (hash of the compiled workspace) and policy, which auto-approves a cheap staging VM.
        val proposed = intents.save(Intent(tenantId = tenant.id, spec = objectMapper.convertValue(draft)))
        val intent = tx.execute { planner.planAndDecide(intents.findById(proposed.id).get()) }!!
        assertEquals(IntentState.APPROVED, intent.state)
        return intent.also { created += Claim(0, it.id, it.tenantId, it.spec, 0, 0) }
    }

    private fun docker(vararg args: String): String {
        val p = ProcessBuilder("docker", *args).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        check(p.waitFor(60, TimeUnit.SECONDS) && p.exitValue() == 0) { "docker ${args.first()} failed: $out" }
        return out
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
    fun `leftovers OpenTofu never saved are adopted into its state, so destroying the intent removes them`() {
        val intent = approvedIntent()
        // What a worker killed mid-apply leaves behind: a volume and a container that exist, and no saved state.
        docker("volume", "create", "--label", "managed_by=linfra", "--label", "intent_id=${intent.id}", "linfra-${intent.id}")
        val leftover = docker(
            "run", "-d", "--name", "linfra-${intent.id}", "--label", "managed_by=linfra", "--label", "intent_id=${intent.id}",
            "--mount", "source=linfra-${intent.id},target=/data", "alpine:3.20", "sleep", "infinity",
        ).trim().lines().last() // the id; docker may print a platform warning first

        worker.processNext("it")

        assertEquals(IntentState.SUCCEEDED, intents.findById(intent.id).get().state)
        assertEquals(leftover, resources.findByIntentId(intent.id)!!.cloudId)
        val state = jdbc.queryForObject("select data from terraform_remote_state.states where name = ?", String::class.java, "intent-${intent.id}")!!
        val inState = objectMapper.readTree(state)["resources"].map { it["type"].asText() + "." + it["name"].asText() }
        assertEquals(setOf("docker_container.vm", "docker_volume.disk"), inState.toSet())

        provisioner.destroy(created.single())
        assertEquals(emptyList(), containersFor(intent.id))
        assertEquals("", docker("volume", "ls", "-q", "--filter", "label=intent_id=${intent.id}").trim())
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
