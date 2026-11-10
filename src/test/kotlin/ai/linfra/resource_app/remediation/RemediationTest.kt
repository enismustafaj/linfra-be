package ai.linfra.resource_app.remediation

import ai.linfra.resource_app.IntegrationTest
import ai.linfra.resource_app.conversation.model.Draft
import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.intent.persistence.Intent
import ai.linfra.resource_app.ranker.RankerFixtures
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import ai.linfra.resource_app.provider.model.ObservedResource
import ai.linfra.resource_app.ranker.model.Provider
import ai.linfra.resource_app.reconciler.service.Reconciler
import ai.linfra.resource_app.remediation.model.FindingStatus
import ai.linfra.resource_app.remediation.model.FindingType
import ai.linfra.resource_app.remediation.persistence.Finding
import ai.linfra.resource_app.remediation.service.RemediationService
import ai.linfra.resource_app.resource.persistence.Resource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

/** Findings are produced the real way: the reconciler runs against a mocked provider listing. */
abstract class RemediationTest : IntegrationTest() {

    @Autowired lateinit var reconciler: Reconciler
    @Autowired lateinit var remediation: RemediationService
    @Autowired lateinit var objectMapper: ObjectMapper

    fun observed(cloudId: String, intent: Intent?, memoryGb: Int = 4) = ObservedResource(
        Provider.DOCKER, cloudId, intent?.id, intent?.tenantId, "burst.medium", "local-eu", 2, memoryGb, false,
    )

    fun recorded(intent: Intent, cloudId: String = "c-${UUID.randomUUID()}"): Resource = resources.save(
        Resource(
            tenantId = intent.tenantId, intentId = intent.id, provider = Provider.DOCKER, region = "local-eu",
            instanceType = "burst.medium", cloudId = cloudId, outputs = emptyMap(),
        )
    )

    /** An intent whose spec is a real draft (burst.medium in local-eu), in the given state. */
    fun draftIntent(state: IntentState): Intent {
        val draft = Draft(RankerFixtures.base, RankerFixtures.ranker.pick(RankerFixtures.base)!!, "why")
        return intents.save(Intent(tenantId = tenant().id, spec = objectMapper.convertValue(draft), state = state))
    }

    fun reconcileWith(vararg observed: ObservedResource) {
        whenever(finder.listManaged()) doReturn observed.toList()
        reconciler.reconcile()
    }

    fun open(type: FindingType): Finding = findings.findAllByStatus(FindingStatus.OPEN).single { it.type == type }

    fun reload(f: Finding): Finding = findings.findById(f.id).get()
}
