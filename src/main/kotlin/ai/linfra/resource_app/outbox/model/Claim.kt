package ai.linfra.resource_app.outbox.model

import java.util.UUID

/** What a worker holds while it acts. Every write back to the DB must present the same fencing token. */
data class Claim(
    val jobId: Long,
    val intentId: UUID,
    val tenantId: UUID,
    val spec: Map<String, Any?>,
    val fencingToken: Long,
    val attempt: Int,
)
