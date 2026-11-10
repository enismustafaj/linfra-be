package ai.linfra.resource_app.provider.exception

import java.util.UUID

/** More than one resource carries the same intent_id: the at-most-one guarantee was broken. Never adopt either. */
class DuplicateResourceException(intentId: UUID, ids: List<String>) :
    RuntimeException("Intent $intentId has ${ids.size} resources: $ids")
