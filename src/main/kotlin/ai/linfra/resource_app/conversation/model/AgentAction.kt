package ai.linfra.resource_app.conversation.model

enum class AgentAction {
    /** Not enough information yet; message holds the question. */
    ASK,
    /** requirements holds a complete VM request. */
    PROPOSE,
    /** Out of scope (anything other than a single VM). */
    REJECT,
}
