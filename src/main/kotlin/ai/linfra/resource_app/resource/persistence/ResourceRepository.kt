package ai.linfra.resource_app.resource.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ResourceRepository: JpaRepository<ResourceEntity, UUID> {
    
    @Query("select r from ResourceEntity r where userId = :userId")
    fun findByUserId(userId: UUID): List<ResourceEntity>
}