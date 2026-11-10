package ai.linfra.resource_app.reconciler.model

import java.util.UUID

/** A Linfra-labeled resource with no record and no worker responsible for it. Reported, never deleted automatically. */
data class Orphan(val cloudId: String, val intentId: UUID?, val tenantId: UUID?, val reason: String)
