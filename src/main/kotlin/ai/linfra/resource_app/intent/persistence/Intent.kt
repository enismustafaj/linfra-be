package ai.linfra.resource_app.intent.persistence

import ai.linfra.resource_app.intent.model.IntentState
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "intents")
class Intent(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "tenant_id")
    val tenantId: UUID,

    @JdbcTypeCode(SqlTypes.JSON)
    val spec: Map<String, Any?>,

    @Enumerated(EnumType.STRING)
    var state: IntentState = IntentState.PROPOSED,

    @Column(name = "plan_hash")
    var planHash: String? = null,

    @Column(name = "approved_at")
    var approvedAt: Instant? = null,

    @Column(name = "approval_expires_at")
    var approvalExpiresAt: Instant? = null,

    /** Why the policy rejected it, why it waits for a human, or who approved it. */
    @Column(name = "decision_reason")
    var decisionReason: String? = null,

    /** Set when the intent came from chat; null for other proposers. */
    @Column(name = "conversation_id")
    val conversationId: UUID? = null,

    /** The assistant message whose draft the user confirmed. Unique, so one draft yields at most one intent. */
    @Column(name = "source_message_id")
    val sourceMessageId: Long? = null,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),

    /** Optimistic lock: two concurrent transitions on the same intent cannot both commit. */
    @Version
    var version: Long = 0,
) {
    fun moveTo(next: IntentState) {
        check(state.canMoveTo(next)) { "Illegal transition $state -> $next for intent $id" }
        state = next
        updatedAt = Instant.now()
    }
}
