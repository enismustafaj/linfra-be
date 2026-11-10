package ai.linfra.resource_app.conversation.model

/** What the assistant says back: always text, plus a draft only when the proposal passed validation and ranking. */
data class AssistantReply(val content: String, val draft: Draft? = null)
