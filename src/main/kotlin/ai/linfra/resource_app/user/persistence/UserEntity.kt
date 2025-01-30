package ai.linfra.resource_app.user.persistence

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "users")
data class UserEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "full_name", nullable = false)
    val fullName: String = "",

    @Column(name = "email", nullable = false)
    val email: String = "",

    @Column(name = "reference_id", nullable = false)
    val referenceId: String = "",

    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(name = "last_modified_at")
    var updatedAt: LocalDateTime  = LocalDateTime.now()
) {
}