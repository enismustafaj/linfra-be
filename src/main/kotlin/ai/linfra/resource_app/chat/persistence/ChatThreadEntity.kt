package ai.linfra.resource_app.chat.persistence

import ai.linfra.resource_app.chat.domain.model.ChatRoleEnum
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "chat_thread")
@EntityListeners(AuditingEntityListener::class)
data class ChatThreadEntity(

    @Id
    @Column(name = "chat_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    val chatId: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    val threadId: ThreadEntity? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    val role: ChatRoleEnum = ChatRoleEnum.user,

    @Column(name = "content", nullable = false)
    val content: String = "",

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(name = "last_modified_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()

)
