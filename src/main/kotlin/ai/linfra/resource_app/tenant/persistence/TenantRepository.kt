package ai.linfra.resource_app.tenant.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TenantRepository : JpaRepository<Tenant, UUID> {
    fun findByApiKeyHash(apiKeyHash: String): Tenant?
}
