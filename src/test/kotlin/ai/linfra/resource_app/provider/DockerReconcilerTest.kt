package ai.linfra.resource_app.provider

import ai.linfra.resource_app.TestcontainersConfiguration
import ai.linfra.resource_app.conversation.model.Draft
import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.intent.persistence.Intent
import ai.linfra.resource_app.intent.persistence.IntentRepository
import ai.linfra.resource_app.intent.service.IntentService
import ai.linfra.resource_app.outbox.model.Claim
import ai.linfra.resource_app.outbox.service.OutboxWorker
import ai.linfra.resource_app.provider.service.Provisioner
import ai.linfra.resource_app.ranker.RankerFixtures
import ai.linfra.resource_app.reconciler.service.Reconciler
import ai.linfra.resource_app.remediation.model.FindingStatus
import ai.linfra.resource_app.remediation.model.FindingType
import ai.linfra.resource_app.remediation.model.RemediationAction
import ai.linfra.resource_app.remediation.persistence.FindingRepository
import ai.linfra.resource_app.remediation.service.RemediationService
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
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The reconciler against real Docker: drift via `docker update`, missing via `docker rm`, an orphan via `docker run`. */
@Tag("tofu")
@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["linfra.worker.enabled=false", "linfra.reconciler.enabled=false"])
class DockerReconcilerTest {

    @Autowired lateinit var tenants: TenantRepository
    @Autowired lateinit var intents: IntentRepository
    @Autowired lateinit var intentService: IntentService
    @Autowired lateinit var resources: ResourceRepository
    @Autowired lateinit var worker: OutboxWorker
    @Autowired lateinit var provisioner: Provisioner
    @Autowired lateinit var reconciler: Reconciler
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var findings: FindingRepository
    @Autowired lateinit var remediation: RemediationService

    private val created = mutableListOf<Claim>()
    private val orphanName = "linfra-orphan-${UUID.randomUUID()}"

    private fun provisioned(): Intent {
        val tenant = tenants.save(Tenant(name = "t", apiKeyHash = UUID.randomUUID().toString()))
        val draft = Draft(RankerFixtures.base, RankerFixtures.ranker.pick(RankerFixtures.base)!!, "why")
        val intent = intents.save(Intent(tenantId = tenant.id, spec = objectMapper.convertValue(draft), state = IntentState.PLANNED, planHash = "h"))
        intentService.approve(tenant.id, intent.id, "h", Duration.ofMinutes(10))
        created += Claim(0, intent.id, intent.tenantId, intent.spec, 0, 0)
        worker.processNext("it")
        assertEquals(IntentState.SUCCEEDED, intents.findById(intent.id).get().state)
        return intent
    }

    private fun docker(vararg args: String) {
        val p = ProcessBuilder("docker", *args).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        check(p.waitFor(60, TimeUnit.SECONDS) && p.exitValue() == 0) { "docker ${args.first()} failed: $out" }
    }

    @AfterEach
    fun cleanUp() {
        runCatching { docker("rm", "-f", orphanName) }
        created.forEach { runCatching { provisioner.destroy(it) } }
        created.forEach { c -> resources.findByIntentId(c.intentId)?.let { it.status = ResourceStatus.DESTROYED; resources.save(it) } }
    }

    @Test
    fun `reports drift, missing and orphans found in real Docker`() {
        val resized = resources.findByIntentId(provisioned().id)!!
        val removed = resources.findByIntentId(provisioned().id)!!

        docker("update", "--memory", "8g", "--memory-swap", "8g", resized.cloudId)
        docker("rm", "-f", removed.cloudId)
        docker("run", "-d", "--name", orphanName, "--label", "managed_by=linfra", "--label", "intent_id=${UUID.randomUUID()}", "alpine:3.20", "sleep", "infinity")

        val report = reconciler.reconcile()

        assertEquals("8", report.drifted.single { it.resourceId == resized.id }.differences["memory_gb"]?.actual)
        // Only this test's resources: other test classes share the database.
        assertTrue(removed.id in report.missing.map { it.resourceId })
        assertTrue(resized.id !in report.missing.map { it.resourceId })
        assertEquals(ResourceStatus.MISSING, resources.findById(removed.id).get().status)
        assertTrue(report.orphans.any { it.reason.startsWith("no resource recorded") }, "${report.orphans}")
        assertTrue(report.duplicates.isEmpty())
    }

    @Test
    fun `a user destroys a reported orphan and the next run finds nothing`() {
        val tenant = tenants.save(Tenant(name = "t", apiKeyHash = UUID.randomUUID().toString()))
        docker("run", "-d", "--name", orphanName, "--label", "managed_by=linfra", "--label", "intent_id=${UUID.randomUUID()}",
            "--label", "tenant_id=${tenant.id}", "alpine:3.20", "sleep", "infinity")
        reconciler.reconcile()
        val orphan = findings.findAllByTenantIdAndStatusOrderByFirstSeenAtDesc(tenant.id, FindingStatus.OPEN).single { it.type == FindingType.ORPHAN }

        remediation.resolve(tenant.id, orphan.id, RemediationAction.DESTROY, null)

        val ps = ProcessBuilder("docker", "ps", "-aq", "--filter", "name=$orphanName").start()
        assertEquals("", ps.inputStream.bufferedReader().readText().trim())
        assertTrue(reconciler.reconcile().orphans.none { it.cloudId in orphan.cloudIds })
    }
}
