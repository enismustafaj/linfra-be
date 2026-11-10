package ai.linfra.resource_app

import ai.linfra.resource_app.conversation.persistence.ConversationRepository
import ai.linfra.resource_app.conversation.persistence.MessageRepository
import ai.linfra.resource_app.conversation.service.AgentClient
import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.intent.persistence.Intent
import ai.linfra.resource_app.intent.persistence.IntentRepository
import ai.linfra.resource_app.intent.service.IntentService
import ai.linfra.resource_app.outbox.model.Claim
import ai.linfra.resource_app.outbox.persistence.OutboxJobRepository
import ai.linfra.resource_app.outbox.persistence.RunRepository
import ai.linfra.resource_app.outbox.service.OutboxStore
import ai.linfra.resource_app.provider.model.ProvisionedResource
import ai.linfra.resource_app.provider.service.DockerContainerFinder
import ai.linfra.resource_app.provider.service.Provisioner
import ai.linfra.resource_app.ranker.model.Provider
import ai.linfra.resource_app.resource.persistence.ResourceRepository
import ai.linfra.resource_app.tenant.persistence.Tenant
import ai.linfra.resource_app.tenant.persistence.TenantRepository
import ai.linfra.resource_app.tenant.security.hashApiKey
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["linfra.worker.enabled=false", "linfra.reconciler.enabled=false", "linfra.worker.max-attempts=2"])
@AutoConfigureMockMvc
abstract class IntegrationTest {

    @Autowired lateinit var tenants: TenantRepository
    @Autowired lateinit var intents: IntentRepository
    @Autowired lateinit var jobs: OutboxJobRepository
    @Autowired lateinit var runs: RunRepository
    @Autowired lateinit var intentService: IntentService
    @Autowired lateinit var store: OutboxStore
    @Autowired lateinit var resources: ResourceRepository
    @Autowired lateinit var conversations: ConversationRepository
    @Autowired lateinit var messages: MessageRepository
    @Autowired lateinit var mvc: MockMvc
    @MockitoBean lateinit var provisioner: Provisioner
    @MockitoBean lateinit var agent: AgentClient
    @MockitoBean lateinit var finder: DockerContainerFinder

    val lease: Duration = Duration.ofSeconds(30)

    @BeforeEach
    fun cleanDatabase() {
        runs.deleteAll(); jobs.deleteAll(); resources.deleteAll(); intents.deleteAll(); messages.deleteAll(); conversations.deleteAll()
    }

    fun tenant(key: String = UUID.randomUUID().toString()): Tenant =
        tenants.save(Tenant(name = "t", apiKeyHash = hashApiKey(key)))

    fun plannedIntent(tenantId: UUID = tenant().id): Intent =
        intents.save(Intent(tenantId = tenantId, spec = mapOf("size" to "small"), state = IntentState.PLANNED, planHash = "h1"))

    /** A PLANNED intent, approved, so exactly one outbox job exists for it. */
    fun approvedIntent(ttl: Duration = Duration.ofMinutes(10)): Intent {
        val intent = plannedIntent()
        return intentService.approve(intent.tenantId, intent.id, "h1", ttl)
    }

    fun claimFor(intentId: UUID, l: Duration = lease): Claim =
        assertNotNull(store.claim("w", l)).also { assertEquals(intentId, it.intentId) }

    fun provisioned(cloudId: String) =
        ProvisionedResource(Provider.DOCKER, "local-eu", "burst.medium", cloudId, mapOf("ipv4" to "172.17.0.9"))

    fun state(intentId: UUID): IntentState = intents.findById(intentId).get().state

    /** Skip backoff / lease wait so the job is claimable right now. */
    fun makeDueNow(intentId: UUID) {
        val job = jobs.findByIntentId(intentId)!!
        job.availableAt = Instant.now().minusSeconds(1)
        job.leaseUntil = job.leaseUntil?.let { Instant.now().minusSeconds(1) }
        jobs.save(job)
    }
}
