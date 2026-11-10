package ai.linfra.resource_app.outbox.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Runs `threads` workers in this process, each on its own thread with its own worker id, each handling one job at
 * a time. Concurrency between threads (and between processes) is handled by SKIP LOCKED claims and fencing tokens,
 * not by anything in here.
 *
 * Shutdown stops claiming and waits up to one lease for in-flight jobs. Threads still busy after that are abandoned
 * (daemon threads): their leases expire and another worker recovers the jobs, exactly as after a crash.
 */
@Component
@ConditionalOnProperty("linfra.worker.enabled", matchIfMissing = true)
class WorkerPool(
    private val worker: OutboxWorker,
    @Value("\${linfra.worker.id}") private val processId: String,
    @Value("\${linfra.worker.threads}") private val threadCount: Int,
    @Value("\${linfra.worker.poll-interval}") private val pollInterval: Duration,
    @Value("\${linfra.worker.lease}") private val lease: Duration,
) : SmartLifecycle {
    private val log = LoggerFactory.getLogger(javaClass)
    @Volatile private var running = false
    private var threads: List<Thread> = emptyList()

    override fun start() {
        running = true
        threads = (1..threadCount).map { n ->
            val workerId = "$processId-$n"
            Thread({ loop(workerId) }, "outbox-worker-$n").apply { isDaemon = true; start() }
        }
        log.info("Started {} outbox workers ({})", threadCount, processId)
    }

    private fun loop(workerId: String) {
        while (running) {
            try {
                if (!worker.processNext(workerId)) Thread.sleep(pollInterval)
            } catch (e: InterruptedException) {
                return
            } catch (e: Exception) {
                // e.g. database unreachable: keep the thread alive and try again later
                log.error("Worker {} loop error", workerId, e)
                runCatching { Thread.sleep(pollInterval) }
            }
        }
    }

    override fun stop() {
        running = false
        val deadline = System.nanoTime() + lease.toNanos()
        threads.forEach { it.join(Duration.ofNanos((deadline - System.nanoTime()).coerceAtLeast(1))) }
        threads.filter { it.isAlive }.forEach { log.warn("{} still busy at shutdown; its lease will expire and the job be recovered", it.name) }
    }

    override fun isRunning() = running
}
