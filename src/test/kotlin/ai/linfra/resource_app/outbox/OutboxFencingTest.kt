package ai.linfra.resource_app.outbox

import ai.linfra.resource_app.IntegrationTest
import ai.linfra.resource_app.intent.model.IntentState
import ai.linfra.resource_app.outbox.exception.LostLeaseException
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class OutboxFencingTest : IntegrationTest() {

    @Test
    fun `a worker whose lease was taken over cannot commit`() {
        val intent = approvedIntent()
        val stale = claimFor(intent.id, Duration.ZERO) // lease already expired: worker "died" mid-apply
        val current = claimFor(intent.id)
        assertEquals(stale.fencingToken + 1, current.fencingToken)

        assertFailsWith<LostLeaseException> { store.succeed(stale, provisioned("srv-stale")) }
        assertFailsWith<LostLeaseException> { store.fail(stale, "boom", maxAttempts = 2) }
        assertFalse(store.heartbeat(stale, lease))
        assertEquals(IntentState.APPLYING, state(intent.id))

        store.succeed(current, provisioned("srv-1"))
        assertEquals(IntentState.SUCCEEDED, state(intent.id))
    }
}
