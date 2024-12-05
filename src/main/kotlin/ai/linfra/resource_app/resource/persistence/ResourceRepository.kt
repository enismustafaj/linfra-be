package ai.linfra.resource_app.resource.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ResourceRepository: JpaRepository<ResourceEntity, UUID> {
}