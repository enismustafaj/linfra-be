package ai.linfra.resource_app.policy.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "policies")
class Policy(
    @Id
    @Column(name = "tenant_id")
    val tenantId: UUID,

    /** Empty means any catalog type. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_instance_types")
    var allowedInstanceTypes: List<String> = emptyList(),

    /** Empty means any catalog region. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_regions")
    var allowedRegions: List<String> = emptyList(),

    @Column(name = "monthly_budget_eur")
    var monthlyBudgetEur: BigDecimal,

    @Column(name = "auto_approve_max_eur")
    var autoApproveMaxEur: BigDecimal,

    @Column(name = "require_approval_for_prod")
    var requireApprovalForProd: Boolean,

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
)
