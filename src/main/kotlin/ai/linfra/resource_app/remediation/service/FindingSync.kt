package ai.linfra.resource_app.remediation.service

import ai.linfra.resource_app.reconciler.model.ReconciliationReport
import ai.linfra.resource_app.remediation.model.FindingDraft
import ai.linfra.resource_app.remediation.model.FindingStatus
import ai.linfra.resource_app.remediation.model.FindingType
import ai.linfra.resource_app.remediation.model.RemediationAction
import ai.linfra.resource_app.remediation.persistence.Finding
import ai.linfra.resource_app.remediation.persistence.FindingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Turns each reconciliation report into the findings table: opens new findings, refreshes open ones, and clears those
 * no longer observed. A subject the user dismissed or accepted is not reopened.
 */
@Service
class FindingSync(private val findings: FindingRepository) {

    private val suppressing = RemediationAction.entries.filter { it.suppressesFinding }

    @Transactional
    fun sync(report: ReconciliationReport) {
        val now = report.checkedAt
        val drafts = drafts(report).associateBy { it.subjectKey }
        val open = findings.findAllByStatus(FindingStatus.OPEN).associateBy { it.subjectKey }

        drafts.values.forEach { d ->
            val existing = open[d.subjectKey]
            if (existing != null) {
                existing.lastSeenAt = now
                existing.details = d.details
                existing.cloudIds = d.cloudIds
            } else if (!findings.existsBySubjectKeyAndStatusAndResolutionIn(d.subjectKey, FindingStatus.RESOLVED, suppressing)) {
                findings.save(
                    Finding(
                        tenantId = d.tenantId, type = d.type, subjectKey = d.subjectKey, intentId = d.intentId,
                        resourceId = d.resourceId, provider = d.provider, cloudIds = d.cloudIds, details = d.details,
                        firstSeenAt = now, lastSeenAt = now,
                    )
                )
            }
        }
        open.filterKeys { it !in drafts }.values.forEach {
            it.status = FindingStatus.CLEARED
            it.resolvedAt = now
        }
    }

    private fun drafts(r: ReconciliationReport): List<FindingDraft> = buildList {
        r.orphans.forEach {
            add(FindingDraft(FindingType.ORPHAN, "ORPHAN:${it.cloudId}", it.tenantId, it.intentId, null, it.provider, listOf(it.cloudId), mapOf("reason" to it.reason)))
        }
        r.duplicates.forEach {
            add(FindingDraft(FindingType.DUPLICATE, "DUPLICATE:${it.intentId}", it.tenantId, it.intentId, null, it.provider, it.cloudIds, emptyMap()))
        }
        r.missing.forEach {
            add(FindingDraft(FindingType.MISSING, "MISSING:${it.resourceId}", it.tenantId, null, it.resourceId, it.provider, listOf(it.cloudId), emptyMap()))
        }
        r.drifted.forEach { d ->
            // The actual values are part of the subject: accepting "memory 4→8" doesn't hide a later "memory 4→16".
            val signature = d.differences.toSortedMap().entries.joinToString(";") { (k, v) -> "$k=${v.actual}" }
            add(
                FindingDraft(
                    FindingType.DRIFT, "DRIFT:${d.resourceId}:$signature", d.tenantId, null, d.resourceId, d.provider,
                    listOf(d.cloudId), mapOf("differences" to d.differences.mapValues { mapOf("expected" to it.value.expected, "actual" to it.value.actual) }),
                )
            )
        }
        r.stuck.forEach {
            add(FindingDraft(FindingType.STUCK, "STUCK:${it.intentId}", it.tenantId, it.intentId, null, null, emptyList(), mapOf("state" to it.state.name, "stuckForSeconds" to it.stuckForSeconds)))
        }
    }
}
