package ai.linfra.resource_app.resource.persistence

import ai.linfra.resource_app.ranker.model.Provider
import ai.linfra.resource_app.resource.model.ResourceStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/** A cloud resource Linfra created. provider/region/instanceType are what we applied: the reconciler compares the cloud against them. */
@Entity
@Table(name = "resources")
class Resource(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "tenant_id")
    val tenantId: UUID,

    @Column(name = "intent_id")
    val intentId: UUID,

    @Enumerated(EnumType.STRING)
    val provider: Provider,

    val region: String,

    @Column(name = "instance_type")
    val instanceType: String,

    @Column(name = "cloud_id")
    val cloudId: String,

    @JdbcTypeCode(SqlTypes.JSON)
    val outputs: Map<String, Any?>,

    @Enumerated(EnumType.STRING)
    var status: ResourceStatus = ResourceStatus.ACTIVE,

    @Column(name = "last_seen_at")
    var lastSeenAt: Instant? = null,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)
