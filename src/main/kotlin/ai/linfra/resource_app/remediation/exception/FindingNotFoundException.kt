package ai.linfra.resource_app.remediation.exception

import java.util.UUID

class FindingNotFoundException(id: UUID) : RuntimeException("Finding $id not found")
