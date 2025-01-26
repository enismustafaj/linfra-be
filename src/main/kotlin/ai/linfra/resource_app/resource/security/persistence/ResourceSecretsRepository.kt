package ai.linfra.resource_app.resource.security.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ResourceSecretsRepository: JpaRepository<ResourceSecretsEntity, UUID> {

    @Query("select rse from ResourceSecretsEntity rse where rse.resourceId = :resourceId")
    fun findOneByResourceId(resourceId: UUID): ResourceSecretsEntity?
}