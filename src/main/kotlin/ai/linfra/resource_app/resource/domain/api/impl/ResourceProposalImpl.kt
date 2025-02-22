package ai.linfra.resource_app.resource.domain.api.impl

import ai.linfra.resource_app.chat.domain.api.UpdateChatThreadUseCase
import ai.linfra.resource_app.chat.domain.model.ChatPrompt
import ai.linfra.resource_app.chat.domain.model.ChatRoleEnum
import ai.linfra.resource_app.resource.domain.api.ResourceProposalUseCase
import ai.linfra.resource_app.resource.domain.dependencies.AiResourceSuggestionModel
import ai.linfra.resource_app.resource.domain.model.ResourceSuggestion
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Component

@Component
class ResourceProposalImpl(
        private val aiResourceSuggestionModel: AiResourceSuggestionModel,
        private val updateChatThreadUseCase: UpdateChatThreadUseCase
) : ResourceProposalUseCase {
    override fun getResourceSuggestions(threadId: String): String {
        val resourceSuggestion: ResourceSuggestion? =
                aiResourceSuggestionModel.suggestResource(threadId)

        if (resourceSuggestion == null || !resourceSuggestion.isValidSuggestion()) {
            return ""
        }

        val resourceSuggestionString =
                Json.encodeToString(ResourceSuggestion.serializer(), resourceSuggestion)

        val resourceManagerChat: ChatPrompt = createChatPrompt(resourceSuggestionString)
        updateChatThreadUseCase.updateThreadConversations(threadId, resourceManagerChat)

        return resourceSuggestionString
    }

    private fun createChatPrompt(content: String): ChatPrompt {
        return ChatPrompt(role = ChatRoleEnum.resource_manager, content = content)
    }
}
