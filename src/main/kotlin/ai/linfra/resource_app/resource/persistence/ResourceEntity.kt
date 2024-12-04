package ai.linfra.resource_app.resource.persistence

import ai.linfra.resource_app.resource.domain.model.ResourceTypeEnum
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import lombok.Data
import org.hibernate.annotations.Type
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

@Data
@Entity
@Table(name = "resource")
@EntityListeners(AuditingEntityListener::class)
class ResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(name = "user_id")
    var userId: UUID? = null

    @Column(name = "resource_name")
    var resourceName: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type")
    var resourceType: ResourceTypeEnum? = null

    @Type(value = JsonType::class)
    @Column(name = "resource_properties", columnDefinition = "json")
    var resourceProperties: List<ResourceProperty>? = null



    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null

    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null

}