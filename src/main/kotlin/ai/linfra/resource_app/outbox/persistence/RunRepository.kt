package ai.linfra.resource_app.outbox.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RunRepository : JpaRepository<Run, Long> {
    fun findByJobIdAndFencingToken(jobId: Long, fencingToken: Long): Run?
    fun findAllByIntentIdOrderById(intentId: UUID): List<Run>
}
