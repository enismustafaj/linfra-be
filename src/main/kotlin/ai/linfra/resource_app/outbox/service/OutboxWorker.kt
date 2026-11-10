package ai.linfra.resource_app.outbox.service

import ai.linfra.resource_app.outbox.exception.LostLeaseException
import ai.linfra.resource_app.provider.service.Provisioner
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration

/** Claims one job, applies it through the provisioner, and records the outcome. Called by each WorkerPool thread. */
@Service
class OutboxWorker(
    private val store: OutboxStore,
    private val heartbeat: LeaseHeartbeat,
    private val provisioner: Provisioner,
    @Value("\${linfra.worker.lease}") private val lease: Duration,
    @Value("\${linfra.worker.max-attempts}") private val maxAttempts: Int,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Returns false when there was nothing to claim, so the caller can back off. */
    fun processNext(workerId: String): Boolean {
        val claim = store.claim(workerId, lease) ?: return false
        heartbeat.start(claim).use {
            try {
                // Never apply without checking first: a previous attempt may have created the resource and died.
                val provisioned = provisioner.find(claim) ?: provisioner.apply(claim)
                store.succeed(claim, provisioned)
            } catch (e: LostLeaseException) {
                log.warn(e.message) // the new lease holder owns the outcome now
            } catch (e: Exception) {
                log.error("Job {} attempt {} failed", claim.jobId, claim.attempt, e)
                runCatching { store.fail(claim, e.toString(), maxAttempts) }
                    .onFailure { log.warn("Could not record failure for job {}: {}", claim.jobId, it.message) }
            }
        }
        return true
    }
}
