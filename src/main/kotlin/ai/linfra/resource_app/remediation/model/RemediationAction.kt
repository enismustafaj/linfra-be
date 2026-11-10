package ai.linfra.resource_app.remediation.model

/** What a user can do about a finding. Which ones a finding offers depends on its type and current context. */
enum class RemediationAction(val suppressesFinding: Boolean = false) {
    /** Orphan: remove the resource from the provider. */
    DESTROY,
    /** Orphan: record it as the resource of its NEEDS_ATTENTION intent. */
    ADOPT,
    /** Orphan or missing: leave it; the same subject is not reported again. */
    DISMISS(suppressesFinding = true),
    /** Duplicate: keep one resource, remove the others. Needs a cloudId. */
    KEEP,
    /** Missing: accept that it's gone; the resource becomes DESTROYED. */
    MARK_DESTROYED,
    /** Drift: the change was intended; this exact drift is not reported again. */
    ACCEPT(suppressesFinding = true),
    /** Stuck: make the job claimable immediately. */
    RETRY_NOW,
    /** Stuck while applying: stop retrying and hand the intent to a human. */
    MARK_NEEDS_ATTENTION,
}
