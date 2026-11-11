package ai.linfra.resource_app.resource.persistence

import ai.linfra.resource_app.resource.model.ResourceStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ResourceRepository : JpaRepository<Resource, UUID> {
    fun findByIntentId(intentId: UUID): Resource?
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): Resource?
    fun findAllByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<Resource>
    fun findAllByStatusIn(statuses: Collection<ResourceStatus>): List<Resource>
    fun findAllByTenantIdAndStatusIn(tenantId: UUID, statuses: Collection<ResourceStatus>): List<Resource>
}
