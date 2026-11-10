package ai.linfra.resource_app.reconciler.service

import ai.linfra.resource_app.reconciler.model.ReconciliationReport
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

/** Gauges over the latest report (operator view, all tenants). NaN until the first run. */
@Component
class ReconcilerMetrics(registry: MeterRegistry, reconciler: Reconciler) {
    init {
        fun gauge(name: String, description: String, value: (ReconciliationReport) -> Number?) =
            Gauge.builder(name) { reconciler.latest()?.let(value)?.toDouble() ?: Double.NaN }
                .description(description).register(registry)

        gauge("linfra.reconciler.orphans", "Linfra-labeled resources with no record and no worker on them") { it.orphans.size }
        gauge("linfra.reconciler.missing", "Recorded resources the provider no longer has") { it.missing.size }
        gauge("linfra.reconciler.drifted", "Resources that differ from what was applied") { it.drifted.size }
        gauge("linfra.reconciler.duplicates", "Intents with more than one resource") { it.duplicates.size }
        gauge("linfra.reconciler.stuck.intents", "In-flight intents with no live lease past the grace period") { it.stuck.size }
        gauge("linfra.reconciler.oldest.unfinished.intent.seconds", "Age of the oldest APPROVED or APPLYING intent") { it.oldestUnfinishedIntentAgeSeconds ?: 0 }
        gauge("linfra.reconciler.last.run.epoch.seconds", "When the last reconciliation ran") { it.checkedAt.epochSecond }
    }
}
