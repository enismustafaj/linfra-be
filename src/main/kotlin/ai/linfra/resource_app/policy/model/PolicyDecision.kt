package ai.linfra.resource_app.policy.model

/** The outcome of checking one planned intent against its tenant's policy. */
sealed interface PolicyDecision {
    data object AutoApprove : PolicyDecision

    /** Allowed, but a human has to approve it (e.g. above the auto-approve limit, or production). */
    data class NeedsApproval(val reason: String) : PolicyDecision

    /** Breaks a hard rule; no human can approve it. */
    data class Reject(val reason: String) : PolicyDecision
}
