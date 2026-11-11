package ai.linfra.resource_app.tenant.persistence

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface TenantRepository : JpaRepository<Tenant, UUID> {
    fun findByApiKeyHash(apiKeyHash: String): Tenant?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tenant t where t.id = :id")
    fun lockById(id: UUID): Tenant
}
