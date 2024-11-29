package ai.linfra.resource_app.user.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.UUID

@Entity
data class WaitListUserEntity(

    @Id
    val id: UUID? = null,

    @Column(name = "email", nullable = false)
    val email: String = "",

    @Column(name = "full_name", nullable = false)
    val fullName: String = ""
)
