package ai.linfra.resource_app.outbox.model

import java.util.UUID

data class Claim(
    val jobId: Long,
    val intentId: UUID,
    val tenantId: UUID,
    val spec: Map<String, Any?>,
    val fencingToken: Long,
    val attempt: Int,
    val planHash: String? = null,
)
