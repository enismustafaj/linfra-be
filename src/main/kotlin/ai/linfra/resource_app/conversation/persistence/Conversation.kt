package ai.linfra.resource_app.conversation.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "conversations")
class Conversation(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "tenant_id")
    val tenantId: UUID,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),

    /** The start of the first message; null until the user writes something. */
    var title: String? = null,
)
