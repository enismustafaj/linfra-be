package ai.linfra.resource_app.chat.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ThreadRepository: JpaRepository<ThreadEntity, UUID> {
}