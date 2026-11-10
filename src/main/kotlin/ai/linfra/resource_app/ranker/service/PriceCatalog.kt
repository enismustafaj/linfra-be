package ai.linfra.resource_app.ranker.service

import ai.linfra.resource_app.ranker.model.Architecture
import ai.linfra.resource_app.ranker.model.Architecture.ARM
import ai.linfra.resource_app.ranker.model.Architecture.X86
import ai.linfra.resource_app.ranker.model.Area
import ai.linfra.resource_app.ranker.model.CatalogEntry
import ai.linfra.resource_app.ranker.model.CpuModel
import ai.linfra.resource_app.ranker.model.CpuModel.BURSTABLE
import ai.linfra.resource_app.ranker.model.CpuModel.DEDICATED
import ai.linfra.resource_app.ranker.model.Provider
import ai.linfra.resource_app.ranker.model.StorageKind
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * The local Docker "cloud". Sizes, CPU model, architecture and storage become real container settings;
 * prices and regions are simulated (every region runs on the local Docker daemon).
 */
@Component
class PriceCatalog {
    // ponytail: hardcoded and simulated; a real provider would refresh this from its pricing API
    val entries: List<CatalogEntry> = listOf("local-eu" to Area.EU, "local-us" to Area.US).flatMap { (region, area) ->
        val discount = if (area == Area.US) BigDecimal("0.9") else BigDecimal.ONE
        listOf(
            docker("burst.small", region, area, 2, 2, BURSTABLE, X86, "10.00", discount),
            docker("burst.medium", region, area, 2, 4, BURSTABLE, X86, "20.00", discount),
            docker("burst.medium.arm", region, area, 2, 4, BURSTABLE, ARM, "16.00", discount),
            docker("dedicated.medium", region, area, 2, 4, DEDICATED, X86, "45.00", discount),
            docker("dedicated.large", region, area, 4, 8, DEDICATED, X86, "90.00", discount),
            docker("storage.medium", region, area, 2, 8, DEDICATED, X86, "70.00", discount, localDiskGb = 100),
        )
    }

    private fun docker(
        type: String, region: String, area: Area, vcpu: Int, memoryGb: Int, cpu: CpuModel, arch: Architecture,
        price: String, discount: BigDecimal, localDiskGb: Int = 0,
    ) = CatalogEntry(
        Provider.DOCKER, type, region, area, vcpu, memoryGb, cpu, arch,
        storage = if (localDiskGb > 0) StorageKind.LOCAL_NVME else StorageKind.NETWORK,
        includedDiskGb = localDiskGb, diskEurPerGb = BigDecimal("0.08"),
        includedEgressGb = 0, egressEurPerGb = BigDecimal("0.08"),
        ipv4EurPerMonth = BigDecimal("3.00"), instanceEurPerMonth = BigDecimal(price) * discount,
    )
}
