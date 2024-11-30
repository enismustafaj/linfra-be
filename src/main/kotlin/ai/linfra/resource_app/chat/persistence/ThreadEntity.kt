package ai.linfra.resource_app.chat.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.UUID

@Entity
data class ThreadEntity(

    @Id
    @Column(name = "thread_id", nullable = false)
    val threadId: UUID? = null,

    @Column(name = "thread_name", nullable = false)
    val threadName: String = "",

    @Column(name = "user_id", nullable = false)
    val userId: String = ""
)
