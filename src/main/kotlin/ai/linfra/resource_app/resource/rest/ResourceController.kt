package ai.linfra.resource_app.resource.rest

import ai.linfra.resource_app.resource.exception.ResourceNotFoundException
import ai.linfra.resource_app.resource.persistence.ResourceRepository
import ai.linfra.resource_app.resource.rest.dto.ResourceResponse
import ai.linfra.resource_app.tenant.model.TenantContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/resources")
class ResourceController(private val resources: ResourceRepository) {

    @GetMapping
    fun list(tenant: TenantContext) = resources.findAllByTenantIdOrderByCreatedAtDesc(tenant.tenantId).map(ResourceResponse::of)

    @GetMapping("/{id}")
    fun get(tenant: TenantContext, @PathVariable id: UUID) =
        ResourceResponse.of(resources.findByIdAndTenantId(id, tenant.tenantId) ?: throw ResourceNotFoundException(id))
}
