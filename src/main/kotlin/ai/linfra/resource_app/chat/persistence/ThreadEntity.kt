package ai.linfra.resource_app.chat.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "thread")
data class ThreadEntity(

    @Id
    @Column(name = "thread_id", nullable = false)
    val threadId: UUID? = null,

    @Column(name = "thread_name", nullable = false)
    val threadName: String = "",

    @Column(name = "user_id", nullable = false)
    val userId: String = ""
)
