package ai.linfra.resource_app.outbox.persistence

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import java.time.Instant
import java.util.UUID

interface OutboxJobRepository : JpaRepository<OutboxJob, Long> {

    /**
     * Claimable = due PENDING jobs, or LEASED jobs whose lease ran out (the worker died or stalled).
     * Lock timeout -2 is Hibernate's SKIP LOCKED: concurrent workers skip rows another worker is claiming
     * instead of blocking on them or claiming the same row.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query(
        """
        select j from OutboxJob j
        where (j.status = ai.linfra.resource_app.outbox.model.JobStatus.PENDING and j.availableAt <= :now)
           or (j.status = ai.linfra.resource_app.outbox.model.JobStatus.LEASED and j.leaseUntil < :now)
        order by j.availableAt
        """
    )
    fun findClaimable(now: Instant, page: Pageable): List<OutboxJob>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from OutboxJob j where j.id = :id")
    fun lockById(id: Long): OutboxJob

    fun findByIntentId(intentId: UUID): OutboxJob?
}
