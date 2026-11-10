package ai.linfra.resource_app.remediation.rest

import ai.linfra.resource_app.remediation.exception.FindingNotFoundException
import ai.linfra.resource_app.remediation.model.FindingStatus
import ai.linfra.resource_app.remediation.persistence.Finding
import ai.linfra.resource_app.remediation.persistence.FindingRepository
import ai.linfra.resource_app.remediation.rest.dto.FindingResponse
import ai.linfra.resource_app.remediation.rest.dto.ResolveFindingRequest
import ai.linfra.resource_app.remediation.service.RemediationOptions
import ai.linfra.resource_app.remediation.service.RemediationService
import ai.linfra.resource_app.tenant.model.TenantContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/findings")
class FindingController(
    private val findings: FindingRepository,
    private val options: RemediationOptions,
    private val remediation: RemediationService,
) {
    @GetMapping
    fun list(tenant: TenantContext, @RequestParam(required = false) status: FindingStatus?) =
        (status?.let { findings.findAllByTenantIdAndStatusOrderByFirstSeenAtDesc(tenant.tenantId, it) }
            ?: findings.findAllByTenantIdOrderByFirstSeenAtDesc(tenant.tenantId)).map(::response)

    @GetMapping("/{id}")
    fun get(tenant: TenantContext, @PathVariable id: UUID) =
        response(findings.findByIdAndTenantId(id, tenant.tenantId) ?: throw FindingNotFoundException(id))

    @PostMapping("/{id}/actions")
    fun act(tenant: TenantContext, @PathVariable id: UUID, @RequestBody body: ResolveFindingRequest) =
        response(remediation.resolve(tenant.tenantId, id, body.action, body.cloudId))

    private fun response(f: Finding) = FindingResponse.of(f, options.actionsFor(f))
}
