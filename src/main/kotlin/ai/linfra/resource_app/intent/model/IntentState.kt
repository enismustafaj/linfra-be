package ai.linfra.resource_app.intent.model

enum class IntentState {
    PROPOSED, PLANNED, APPROVED, APPLYING, SUCCEEDED,
    REJECTED, EXPIRED, FAILED, NEEDS_ATTENTION;

    fun canMoveTo(next: IntentState): Boolean = next in (TRANSITIONS[this] ?: emptySet())

    companion object {
        private val TRANSITIONS = mapOf(
            PROPOSED to setOf(PLANNED, REJECTED),
            PLANNED to setOf(APPROVED, REJECTED, EXPIRED),
            APPROVED to setOf(APPLYING, EXPIRED),
            APPLYING to setOf(SUCCEEDED, FAILED, NEEDS_ATTENTION),
            EXPIRED to setOf(PLANNED), // re-plan, then re-approve
            // A human decides: adopt what exists (SUCCEEDED) or give up (FAILED). Only through a finding's action.
            NEEDS_ATTENTION to setOf(SUCCEEDED, FAILED),
            // SUCCEEDED, REJECTED and FAILED are terminal
        )
    }
}
