package ai.linfra.resource_app.resource.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ResourceRepository: JpaRepository<ResourceEntity, UUID> {
    
    @Query("select r from ResourceEntity r where r.userId = :userId")
    fun findByUserId(userId: String): List<ResourceEntity>

    @Query("select r from ResourceEntity r where r.chatThreadId = :chatThreadId")
    fun findByChatThreadId(chatThreadId: String): ResourceEntity
}