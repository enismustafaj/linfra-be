package ai.linfra.resource_app.intent.rest

import ai.linfra.resource_app.intent.exception.IntentNotFoundException
import ai.linfra.resource_app.intent.persistence.IntentRepository
import ai.linfra.resource_app.tenant.model.TenantContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/intents")
class IntentController(private val intents: IntentRepository) {

    @GetMapping
    fun list(tenant: TenantContext) = intents.findAllByTenantIdOrderByCreatedAtDesc(tenant.tenantId)

    @GetMapping("/{id}")
    fun get(tenant: TenantContext, @PathVariable id: UUID) =
        intents.findByIdAndTenantId(id, tenant.tenantId) ?: throw IntentNotFoundException(id)
}
