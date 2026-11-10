package ai.linfra.resource_app.reconciler.model

import java.util.UUID

/** More than one resource carries the same intent_id: the at-most-one guarantee was broken. */
data class Duplicate(val intentId: UUID, val tenantId: UUID?, val cloudIds: List<String>)
