package ai.linfra.resource_app.chat.persistence

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "thread")
@EntityListeners(AuditingEntityListener::class)
data class ThreadEntity(

    @Id
    @Column(name = "id", nullable = false)
    val threadId: UUID? = null,

    @Column(name = "thread_name", nullable = false)
    val threadName: String = "",

    @Column(name = "user_id", nullable = false)
    val userId: String = "",

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(name = "last_modified_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
