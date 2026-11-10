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
            // SUCCEEDED, REJECTED, FAILED, NEEDS_ATTENTION are terminal
        )
    }
}
