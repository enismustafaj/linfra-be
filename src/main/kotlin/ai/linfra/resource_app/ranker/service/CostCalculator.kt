package ai.linfra.resource_app.ranker.service

import ai.linfra.resource_app.conversation.model.VmRequirements
import ai.linfra.resource_app.ranker.model.CatalogEntry
import ai.linfra.resource_app.ranker.model.CostBreakdown
import ai.linfra.resource_app.ranker.model.StorageKind
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

private const val DEFAULT_NETWORK_DISK_GB = 20

/** Total monthly cost of running an entry for given requirements: instance + disk + egress + IPv4. */
@Component
class CostCalculator {

    fun diskGb(entry: CatalogEntry, req: VmRequirements): Int =
        if (entry.storage == StorageKind.NETWORK) req.disk?.minGb ?: DEFAULT_NETWORK_DISK_GB else entry.includedDiskGb

    fun cost(entry: CatalogEntry, req: VmRequirements): CostBreakdown {
        val disk = if (entry.storage == StorageKind.NETWORK) entry.diskEurPerGb * diskGb(entry, req).toBigDecimal() else BigDecimal.ZERO
        val billableEgress = (req.estimatedEgressGbPerMonth - entry.includedEgressGb).coerceAtLeast(0)
        val egress = entry.egressEurPerGb * billableEgress.toBigDecimal()
        // ponytail: assumes exactly one public IPv4 per VM; make it a requirement if private-only VMs appear
        val total = entry.instanceEurPerMonth + disk + egress + entry.ipv4EurPerMonth
        return CostBreakdown(entry.instanceEurPerMonth.eur(), disk.eur(), egress.eur(), entry.ipv4EurPerMonth.eur(), total.eur())
    }

    private fun BigDecimal.eur() = setScale(2, RoundingMode.HALF_UP)
}
