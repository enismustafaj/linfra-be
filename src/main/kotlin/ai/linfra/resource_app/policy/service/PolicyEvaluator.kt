package ai.linfra.resource_app.policy.service

import ai.linfra.resource_app.conversation.model.Environment
import ai.linfra.resource_app.policy.model.PolicyCheck
import ai.linfra.resource_app.policy.model.PolicyDecision
import ai.linfra.resource_app.policy.persistence.Policy
import org.springframework.stereotype.Component

/**
 * Pure rules, in order: hard limits first (type, region, budget: rejected, nobody can override), then the reasons a
 * human must look (production, cost above the auto-approve limit), otherwise auto-approve.
 */
@Component
class PolicyEvaluator {

    fun evaluate(policy: Policy, check: PolicyCheck, allowAutoApprove: Boolean = true): PolicyDecision {
        if (policy.allowedInstanceTypes.isNotEmpty() && check.instanceType !in policy.allowedInstanceTypes) {
            return PolicyDecision.Reject("instance type ${check.instanceType} is not allowed (allowed: ${policy.allowedInstanceTypes.joinToString()})")
        }
        if (policy.allowedRegions.isNotEmpty() && check.region !in policy.allowedRegions) {
            return PolicyDecision.Reject("region ${check.region} is not allowed (allowed: ${policy.allowedRegions.joinToString()})")
        }
        val total = check.committedSpendEur + check.monthlyCostEur
        if (total > policy.monthlyBudgetEur) {
            return PolicyDecision.Reject(
                "over the monthly budget: €${check.committedSpendEur} committed + €${check.monthlyCostEur} = €$total > €${policy.monthlyBudgetEur}",
            )
        }
        if (!allowAutoApprove) return PolicyDecision.AutoApprove
        if (policy.requireApprovalForProd && check.environment == Environment.PROD) {
            return PolicyDecision.NeedsApproval("production requests need a human approval")
        }
        if (check.monthlyCostEur > policy.autoApproveMaxEur) {
            return PolicyDecision.NeedsApproval("€${check.monthlyCostEur}/month is above the auto-approve limit of €${policy.autoApproveMaxEur}")
        }
        return PolicyDecision.AutoApprove
    }
}
