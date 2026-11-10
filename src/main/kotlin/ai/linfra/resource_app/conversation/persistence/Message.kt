package ai.linfra.resource_app.conversation.persistence

import ai.linfra.resource_app.conversation.model.Draft
import ai.linfra.resource_app.conversation.model.MessageRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "messages")
class Message(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "conversation_id")
    val conversationId: UUID,

    @Enumerated(EnumType.STRING)
    val role: MessageRole,

    val content: String,

    @JdbcTypeCode(SqlTypes.JSON)
    val draft: Draft? = null,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),
)
