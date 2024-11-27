package ai.linfra.resource_app.conversation.persistence

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "conversation_thread")
data class ConversationThreadEntity(

    @Id
    var id: UUID? = null,

    @Column(name = "thread_name", nullable = false)
    var threadName: String = "",

    @Column(name = "user_id", nullable = false)
    var userId: String = "",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "thread_payload", nullable = false)
    var threadPayload: ThreadPayload = ThreadPayload(mutableListOf()),

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)