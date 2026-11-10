package ai.linfra.resource_app.conversation.exception

import java.util.UUID

class ConversationNotFoundException(id: UUID) : RuntimeException("Conversation $id not found")
