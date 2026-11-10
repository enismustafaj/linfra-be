package ai.linfra.resource_app.conversation.model

enum class TraitSource {
    /** The user said it. Hard constraint: candidates that fail it are excluded. */
    STATED,
    /** The LLM concluded it from the use case. Soft preference: ranks candidates, never excludes them. */
    INFERRED,
}
