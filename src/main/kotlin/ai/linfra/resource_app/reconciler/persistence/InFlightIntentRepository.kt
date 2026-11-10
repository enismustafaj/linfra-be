package ai.linfra.resource_app.reconciler.persistence

import ai.linfra.resource_app.intent.persistence.Intent
import ai.linfra.resource_app.reconciler.model.InFlightIntent
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import java.time.Instant
import java.util.UUID

/**
 * The reconciler's view of in-flight intents, in one query. The intent says what should be happening (APPROVED or
 * APPLYING); its outbox job says whether a worker is on it. Heartbeats extend the job's lease, never the intent, so
 * the intent alone can't tell a long healthy apply from a dead worker.
 */
interface InFlightIntentRepository : Repository<Intent, UUID> {

    @Query(
        """
        select new ai.linfra.resource_app.reconciler.model.InFlightIntent(
            i.id, i.tenantId, i.state,
            coalesce(i.approvedAt, i.createdAt),
            greatest(j.availableAt, coalesce(i.approvedAt, i.createdAt),
                     case when j.status = ai.linfra.resource_app.outbox.model.JobStatus.LEASED then j.leaseUntil end),
            case when j.status = ai.linfra.resource_app.outbox.model.JobStatus.LEASED and j.leaseUntil > :now
                 then j.leaseUntil end
        )
        from Intent i left join OutboxJob j on j.intentId = i.id
        where i.state in (ai.linfra.resource_app.intent.model.IntentState.APPROVED,
                          ai.linfra.resource_app.intent.model.IntentState.APPLYING)
        """
    )
    fun findInFlight(now: Instant): List<InFlightIntent>
}
