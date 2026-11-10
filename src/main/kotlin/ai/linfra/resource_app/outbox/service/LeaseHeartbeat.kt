package ai.linfra.resource_app.outbox.service

import ai.linfra.resource_app.outbox.model.Claim
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Keeps a claim's lease alive while a slow cloud call runs. Close the returned handle when the work ends. */
@Component
class LeaseHeartbeat(
    private val store: OutboxStore,
    @Value("\${linfra.worker.lease}") private val lease: Duration,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val executor = Executors.newSingleThreadScheduledExecutor()

    fun start(claim: Claim): AutoCloseable {
        val every = lease.toMillis() / 3
        val beat = executor.scheduleAtFixedRate({
            runCatching { store.heartbeat(claim, lease) }
                .onSuccess { if (!it) log.warn("Lost lease on job {} (token {})", claim.jobId, claim.fencingToken) }
                .onFailure { log.warn("Heartbeat failed for job {}", claim.jobId, it) }
        }, every, every, TimeUnit.MILLISECONDS)
        return AutoCloseable { beat.cancel(false) }
    }

    @PreDestroy
    fun stop() = executor.shutdownNow()
}
