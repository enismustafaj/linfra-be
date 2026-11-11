package ai.linfra.resource_app.policy.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PolicyRepository : JpaRepository<Policy, UUID>
