package ai.linfra.resource_app.conversation.model

/** No SYSTEM role: the system prompt is code, never stored or client-supplied. */
enum class MessageRole { USER, ASSISTANT }
