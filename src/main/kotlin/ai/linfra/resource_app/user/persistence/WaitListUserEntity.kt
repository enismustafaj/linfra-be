package ai.linfra.resource_app.user.persistence

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity(name = "wait_list_user")
@EntityListeners(AuditingEntityListener::class)
data class WaitListUserEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", unique = true, nullable = false)
    val userId: String = "",

    @Column(name = "email", nullable = false)
    val email: String = "",

    @Column(name = "name", nullable = false)
    val name: String = "",

    @CreatedDate
    @Column(name = "created_date")
    val createdDate: LocalDateTime? = null,

    @LastModifiedDate
    @Column(name = "last_modified_date")
    val lastModifiedDate: LocalDateTime? = null
)
