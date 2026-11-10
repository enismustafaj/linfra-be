package ai.linfra.resource_app.ranker.model

import java.math.BigDecimal

data class CatalogEntry(
    val provider: Provider,
    val instanceType: String,
    val region: String,
    val area: Area,
    val vcpu: Int,
    val memoryGb: Int,
    val cpuModel: CpuModel,
    val architecture: Architecture,
    val storage: StorageKind,
    val includedDiskGb: Int,
    val diskEurPerGb: BigDecimal,
    val includedEgressGb: Int,
    val egressEurPerGb: BigDecimal,
    val ipv4EurPerMonth: BigDecimal,
    val instanceEurPerMonth: BigDecimal,
)
