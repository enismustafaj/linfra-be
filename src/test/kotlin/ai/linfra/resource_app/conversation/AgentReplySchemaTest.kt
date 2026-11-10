package ai.linfra.resource_app.conversation

import ai.linfra.resource_app.conversation.model.AgentReply
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.ai.converter.BeanOutputConverter

/** OpenAI strict structured output rejects schemas unless every object is closed and lists all its properties as required. */
class AgentReplySchemaTest {

    @Test
    fun `schema sent to OpenAI is strict-mode compatible`() {
        val schema = ObjectMapper().readTree(BeanOutputConverter(AgentReply::class.java).jsonSchema)
        assertStrict(schema, "$")
    }

    private fun assertStrict(node: JsonNode, path: String) {
        val props = node["properties"]
        if (props != null) {
            assert(node["additionalProperties"]?.asBoolean() == false) { "$path: additionalProperties must be false" }
            val required = node["required"]?.map { it.asText() }?.toSet() ?: emptySet()
            assert(required == props.fieldNames().asSequence().toSet()) { "$path: required $required != properties" }
            props.fields().forEach { (name, child) -> assertStrict(child, "$path.$name") }
        }
        node["\$defs"]?.fields()?.forEach { (name, child) -> assertStrict(child, "$path.\$defs.$name") }
    }
}
