package ai.linfra.resource_app.ai.client

import ai.linfra.resource_app.chat.domain.api.GetThreadUseCase
import ai.linfra.resource_app.chat.domain.model.ChatPrompt
import ai.linfra.resource_app.chat.domain.model.ChatRoleEnum
import ai.linfra.resource_app.chat.domain.model.Thread
import ai.linfra.resource_app.resource.domain.dependencies.AiResourceSuggestionModel
import ai.linfra.resource_app.resource.domain.model.ResourceSuggestion
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.ResponseFormat

class AiResourceSuggestionModelClientImpl(
        private val chatModel: ChatModel,
        private val getThreadUseCase: GetThreadUseCase
) : AiResourceSuggestionModel {

    internal val outputConverter: BeanOutputConverter<ResourceSuggestion> =
            BeanOutputConverter(ResourceSuggestion::class.java)

    internal val initSystemprompt: String =
            """
        You are a database infrastructure adivisor. The only database types you support are PostgreSQL and MySQL.
        The database sizes you support are small and medium. Extract the database properties from the
        conversation provided to you.
    """

    override fun suggestResource(threadId: String): ResourceSuggestion? {
        val chatResponse: ChatResponse = chatModel.call(buildRecommendationPrompt(threadId))

        return outputConverter.convert(chatResponse.getResult().getOutput().getText())
    }

    private fun buildRecommendationPrompt(threadId: String): Prompt {
        val thread: Thread = getThreadUseCase.getThread(threadId)
        val messages: List<Message> =
                thread.prompts
                        .filter { chatPrompt ->
                            excludeSystemAndResourceManagerMessages(chatPrompt)
                        }
                        .map { buildPromptMessage(it) }
        return Prompt(messages)
    }

    private fun excludeSystemAndResourceManagerMessages(chatPrompt: ChatPrompt): Boolean {
        return chatPrompt.role != ChatRoleEnum.system &&
                chatPrompt.role != ChatRoleEnum.resource_manager
    }

    private fun buildPromptOptions(): OpenAiChatOptions {
        return OpenAiChatOptions.builder()
                .responseFormat(
                        ResponseFormat(
                                ResponseFormat.Type.JSON_SCHEMA,
                                outputConverter.getJsonSchema()
                        )
                )
                .build()
    }

    private fun buildPromptMessage(chatPrompt: ChatPrompt): Message {
        return if (chatPrompt.role == ChatRoleEnum.user) {
            UserMessage(chatPrompt.content)
        } else {
            AssistantMessage(chatPrompt.content)
        }
    }
}
