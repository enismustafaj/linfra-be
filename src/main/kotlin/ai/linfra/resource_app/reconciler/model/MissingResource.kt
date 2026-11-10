package ai.linfra.resource_app.reconciler.model

import java.util.UUID

/** Recorded as existing, but the provider no longer has it (e.g. removed outside Linfra). */
data class MissingResource(val resourceId: UUID, val tenantId: UUID, val cloudId: String)
