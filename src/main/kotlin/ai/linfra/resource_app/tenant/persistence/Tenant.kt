package ai.linfra.resource_app.tenant.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "tenants")
class Tenant(
    @Id
    val id: UUID = UUID.randomUUID(),
    val name: String,
    @Column(name = "api_key_hash")
    val apiKeyHash: String,
    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),
)
