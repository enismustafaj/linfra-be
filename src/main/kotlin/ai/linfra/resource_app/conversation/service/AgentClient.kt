package ai.linfra.resource_app.conversation.service

import ai.linfra.resource_app.conversation.exception.InvalidAgentOutputException
import ai.linfra.resource_app.conversation.model.AgentReply
import ai.linfra.resource_app.conversation.model.MessageRole
import ai.linfra.resource_app.conversation.model.VmRequirements
import ai.linfra.resource_app.conversation.persistence.Message
import ai.linfra.resource_app.ranker.model.RankedPick
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.ResponseFormat
import org.springframework.stereotype.Service

/** The only class that talks to the LLM. Everything it returns is untrusted until validated by the caller. */
@Service
class AgentClient(builder: ChatClient.Builder, private val objectMapper: ObjectMapper) {

    private val chat = builder.build()
    private val converter = BeanOutputConverter(AgentReply::class.java)

    // OpenAI structured outputs: the model is constrained to this JSON schema.
    private val replyOptions = OpenAiChatOptions.builder()
        .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_SCHEMA).jsonSchema(converter.jsonSchema).build())
        .build()

    fun reply(history: List<Message>): AgentReply {
        val text = chat.prompt()
            .system(AgentPrompts.EXTRACT)
            .messages(history.map { if (it.role == MessageRole.USER) UserMessage(it.content) else AssistantMessage(it.content) })
            .options(replyOptions)
            .call()
            .content()
            ?: throw InvalidAgentOutputException("empty response")
        return try {
            converter.convert(text) ?: throw InvalidAgentOutputException("null after parsing")
        } catch (e: RuntimeException) {
            throw e as? InvalidAgentOutputException ?: InvalidAgentOutputException("unparseable response", e)
        }
    }

    fun explain(requirements: VmRequirements, pick: RankedPick): String =
        chat.prompt()
            .system(AgentPrompts.EXPLAIN)
            .user(objectMapper.writeValueAsString(mapOf("requirements" to requirements, "chosen" to pick)))
            .call()
            .content()
            .orEmpty()
}
