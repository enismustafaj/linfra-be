package ai.linfra.resource_app.resource.rest.dto

import ai.linfra.resource_app.ranker.model.Provider
import ai.linfra.resource_app.resource.model.ResourceStatus
import ai.linfra.resource_app.resource.persistence.Resource
import java.time.Instant
import java.util.UUID

data class ResourceResponse(
    val id: UUID,
    val intentId: UUID,
    val provider: Provider,
    val region: String,
    val instanceType: String,
    val cloudId: String,
    val outputs: Map<String, Any?>,
    val status: ResourceStatus,
    val createdAt: Instant,
    val lastSeenAt: Instant?,
) {
    companion object {
        fun of(r: Resource) = ResourceResponse(
            r.id, r.intentId, r.provider, r.region, r.instanceType, r.cloudId, r.outputs, r.status, r.createdAt, r.lastSeenAt,
        )
    }
}
