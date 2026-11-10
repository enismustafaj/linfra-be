package ai.linfra.resource_app.conversation.model

/** The only shape the LLM can answer in. Parsed from JSON, then validated before anything uses it. */
data class AgentReply(
    val action: AgentAction,
    val message: String,
    val requirements: VmRequirements?,
)
