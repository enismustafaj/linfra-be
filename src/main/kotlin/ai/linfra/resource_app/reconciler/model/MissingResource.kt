package ai.linfra.resource_app.reconciler.model

import ai.linfra.resource_app.ranker.model.Provider
import java.util.UUID

/** Recorded as existing, but the provider no longer has it (e.g. removed outside Linfra). */
data class MissingResource(val resourceId: UUID, val tenantId: UUID, val provider: Provider, val cloudId: String)
