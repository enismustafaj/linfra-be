package ai.linfra.resource_app.reconciler.rest

import ai.linfra.resource_app.reconciler.model.ReconciliationReport
import ai.linfra.resource_app.reconciler.service.Reconciler
import ai.linfra.resource_app.tenant.model.TenantContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/reconciliation")
class ReconciliationController(private val reconciler: Reconciler) {

    /** The latest report, limited to the caller's tenant. 204 until the first run. */
    @GetMapping
    fun latest(tenant: TenantContext): ResponseEntity<ReconciliationReport> =
        reconciler.latest()?.let { ResponseEntity.ok(it.forTenant(tenant.tenantId)) } ?: ResponseEntity.noContent().build()
}
