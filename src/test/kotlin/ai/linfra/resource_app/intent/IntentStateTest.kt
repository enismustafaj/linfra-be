package ai.linfra.resource_app.intent

import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.intent.persistence.Intent

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntentStateTest {

    @Test
    fun `happy path is legal`() {
        val intent = Intent(tenantId = UUID.randomUUID(), spec = mapOf())
        listOf(IntentState.PLANNED, IntentState.APPROVED, IntentState.APPLYING, IntentState.SUCCEEDED).forEach(intent::moveTo)
        assertEquals(IntentState.SUCCEEDED, intent.state)
    }

    @Test
    fun `cannot skip approval or leave a terminal state`() {
        assertFailsWith<IllegalStateException> { Intent(tenantId = UUID.randomUUID(), spec = mapOf()).moveTo(IntentState.APPLYING) }
        IntentState.entries.filter { it in setOf(IntentState.SUCCEEDED, IntentState.FAILED, IntentState.REJECTED, IntentState.NEEDS_ATTENTION) }
            .forEach { terminal -> IntentState.entries.forEach { assert(!terminal.canMoveTo(it)) { "$terminal -> $it" } } }
    }
}
