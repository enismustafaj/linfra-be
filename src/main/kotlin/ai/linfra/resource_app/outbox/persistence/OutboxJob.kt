package ai.linfra.resource_app.outbox.persistence

import ai.linfra.resource_app.outbox.model.JobStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "outbox_jobs")
class OutboxJob(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "intent_id")
    val intentId: UUID,

    @Enumerated(EnumType.STRING)
    var status: JobStatus = JobStatus.PENDING,

    var attempts: Int = 0,

    @Column(name = "available_at")
    var availableAt: Instant = Instant.now(),

    @Column(name = "lease_owner")
    var leaseOwner: String? = null,

    @Column(name = "lease_until")
    var leaseUntil: Instant? = null,

    /** Incremented on every claim. A worker may only write if the token it claimed with is still current. */
    @Column(name = "fencing_token")
    var fencingToken: Long = 0,

    @Column(name = "last_error")
    var lastError: String? = null,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),
)
