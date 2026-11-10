package ai.linfra.resource_app.outbox.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** One row per claim, written before the worker touches a cloud. Rows with no finishedAt are crashed or in-flight runs. */
@Entity
@Table(name = "runs")
class Run(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "intent_id")
    val intentId: UUID,

    @Column(name = "job_id")
    val jobId: Long,

    @Column(name = "fencing_token")
    val fencingToken: Long,

    @Column(name = "worker_id")
    val workerId: String,

    @Column(name = "started_at")
    val startedAt: Instant,

    @Column(name = "finished_at")
    var finishedAt: Instant? = null,

    var outcome: String? = null,
)
