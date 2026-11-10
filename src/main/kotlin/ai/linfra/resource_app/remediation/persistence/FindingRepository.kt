package ai.linfra.resource_app.remediation.persistence

import ai.linfra.resource_app.remediation.model.FindingStatus
import ai.linfra.resource_app.remediation.model.RemediationAction
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FindingRepository : JpaRepository<Finding, UUID> {
    fun findAllByStatus(status: FindingStatus): List<Finding>
    fun findAllByTenantIdAndStatusOrderByFirstSeenAtDesc(tenantId: UUID, status: FindingStatus): List<Finding>
    fun findAllByTenantIdOrderByFirstSeenAtDesc(tenantId: UUID): List<Finding>
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): Finding?
    fun existsBySubjectKeyAndStatusAndResolutionIn(subjectKey: String, status: FindingStatus, resolutions: Collection<RemediationAction>): Boolean
}
