package ai.linfra.resource_app.resource.security.persistence

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import lombok.Data
import org.hibernate.annotations.Type
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.*

@Data
@Entity
@Table(name = "resource_secrets")
@EntityListeners(AuditingEntityListener::class)
class ResourceSecretsEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "resource_id")
    var resourceId: UUID,

    @Type(value = JsonType::class)
    @Column(name = "resource_credentials")
    var credentials: ResourceCredentials,

    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(name = "last_modified_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
}