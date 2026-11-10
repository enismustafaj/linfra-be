package ai.linfra.resource_app.reconciler.config

import ai.linfra.resource_app.reconciler.service.Reconciler
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

/** Runs the reconciler on a fixed delay. Tests set linfra.reconciler.enabled=false and call it directly. */
@Configuration
@EnableScheduling
@ConditionalOnProperty("linfra.reconciler.enabled", matchIfMissing = true)
class ReconcilerSchedule(private val reconciler: Reconciler) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${linfra.reconciler.interval}", initialDelayString = "\${linfra.reconciler.interval}")
    fun run() {
        runCatching { reconciler.reconcile() }.onFailure { log.error("Reconciliation failed", it) }
    }
}
