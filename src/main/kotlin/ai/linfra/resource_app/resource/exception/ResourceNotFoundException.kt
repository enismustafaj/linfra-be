package ai.linfra.resource_app.resource.exception

import java.util.UUID

class ResourceNotFoundException(id: UUID) : RuntimeException("Resource $id not found")
