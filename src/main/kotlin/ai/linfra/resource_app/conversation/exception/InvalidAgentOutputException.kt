package ai.linfra.resource_app.conversation.exception

/** The LLM answered, but not in the agreed shape. Treated as "ask again", never partially accepted. */
class InvalidAgentOutputException(reason: String, cause: Throwable? = null) : RuntimeException(reason, cause)
