package ai.linfra.resource_app.intent.exception

import java.util.UUID

class IntentNotFoundException(id: UUID) : RuntimeException("Intent $id not found")
