package ai.linfra.resource_app.chat.persistence

import ai.linfra.resource_app.chat.domain.model.ChatRoleEnum
import jakarta.persistence.*
import java.util.UUID

@Entity
data class ChatThreadEntity(

    @Id
    @Column(name = "chat_id", nullable = false)
    val chatId: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    val threadId: ThreadEntity? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    val role: ChatRoleEnum = ChatRoleEnum.USER,

    @Column(name = "content", nullable = false)
    val content: String = ""

)
