package ai.linfra.resource_app.remediation.persistence

import ai.linfra.resource_app.ranker.model.Provider
import ai.linfra.resource_app.remediation.model.FindingStatus
import ai.linfra.resource_app.remediation.model.FindingType
import ai.linfra.resource_app.remediation.model.RemediationAction
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "findings")
class Finding(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "tenant_id")
    val tenantId: UUID?,

    @Enumerated(EnumType.STRING)
    val type: FindingType,

    @Column(name = "subject_key")
    val subjectKey: String,

    @Column(name = "intent_id")
    val intentId: UUID?,

    @Column(name = "resource_id")
    val resourceId: UUID?,

    @Enumerated(EnumType.STRING)
    val provider: Provider?,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cloud_ids")
    var cloudIds: List<String>,

    @JdbcTypeCode(SqlTypes.JSON)
    var details: Map<String, Any?>,

    @Enumerated(EnumType.STRING)
    var status: FindingStatus = FindingStatus.OPEN,

    @Enumerated(EnumType.STRING)
    var resolution: RemediationAction? = null,

    @Column(name = "resolution_detail")
    var resolutionDetail: String? = null,

    @Column(name = "first_seen_at")
    val firstSeenAt: Instant,

    @Column(name = "last_seen_at")
    var lastSeenAt: Instant,

    @Column(name = "resolved_at")
    var resolvedAt: Instant? = null,

    /** The reconciler and a user acting on the same finding can't both win. */
    @Version
    var version: Long = 0,
)
