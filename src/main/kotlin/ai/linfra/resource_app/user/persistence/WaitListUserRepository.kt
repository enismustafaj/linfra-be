package ai.linfra.resource_app.user.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface WaitListUserRepository: JpaRepository<WaitListUserEntity, UUID> {
    fun findByUserId(userId: String): Optional<WaitListUserEntity>

}
