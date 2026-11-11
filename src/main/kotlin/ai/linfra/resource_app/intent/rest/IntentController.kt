package ai.linfra.resource_app.intent.rest

import ai.linfra.resource_app.intent.exception.IntentNotFoundException
import ai.linfra.resource_app.intent.persistence.IntentRepository
import ai.linfra.resource_app.intent.service.IntentPlanner
import ai.linfra.resource_app.intent.service.IntentService
import ai.linfra.resource_app.tenant.model.TenantContext
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/intents")
class IntentController(
    private val intents: IntentRepository,
    private val intentService: IntentService,
    private val planner: IntentPlanner,
) {
    @GetMapping
    fun list(tenant: TenantContext) = intents.findAllByTenantIdOrderByCreatedAtDesc(tenant.tenantId)

    @GetMapping("/{id}")
    fun get(tenant: TenantContext, @PathVariable id: UUID) =
        intents.findByIdAndTenantId(id, tenant.tenantId) ?: throw IntentNotFoundException(id)

    /** Plans a PROPOSED or EXPIRED intent and runs the policy on it. */
    @PostMapping("/{id}/plan")
    @Transactional
    fun plan(tenant: TenantContext, @PathVariable id: UUID) =
        planner.planAndDecide(intents.findByIdAndTenantId(id, tenant.tenantId) ?: throw IntentNotFoundException(id))

    @PostMapping("/{id}/approve")
    fun approve(tenant: TenantContext, @PathVariable id: UUID) = intentService.approveByHuman(tenant.tenantId, id)

    @PostMapping("/{id}/reject")
    fun reject(tenant: TenantContext, @PathVariable id: UUID) = intentService.rejectByHuman(tenant.tenantId, id)
}
