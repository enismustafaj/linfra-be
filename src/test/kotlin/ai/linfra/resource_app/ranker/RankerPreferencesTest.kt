package ai.linfra.resource_app.ranker

import ai.linfra.resource_app.conversation.model.TraitSource.INFERRED
import ai.linfra.resource_app.ranker.RankerFixtures.arm
import ai.linfra.resource_app.ranker.RankerFixtures.base
import ai.linfra.resource_app.ranker.RankerFixtures.localNvme
import ai.linfra.resource_app.ranker.RankerFixtures.ranker
import ai.linfra.resource_app.ranker.RankerFixtures.sustained
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** INFERRED traits rank candidates but never exclude them. */
class RankerPreferencesTest {

    @Test
    fun `inferred sustained CPU prefers dedicated cores over a cheaper burstable type`() {
        val pick = ranker.pick(base.copy(cpuProfile = sustained(INFERRED)))!!

        assertEquals("dedicated.medium", pick.entry.instanceType)
        assertEquals(listOf("sustained CPU"), pick.preferencesMet)
    }

    @Test
    fun `an inferred preference that blows the budget is reported as missed, not fatal`() {
        val pick = ranker.pick(base.copy(cpuProfile = sustained(INFERRED), maxMonthlyEur = "40".toBigDecimal()))!!

        assertEquals("burst.medium", pick.entry.instanceType)
        assertEquals(listOf("sustained CPU"), pick.preferencesMissed)
    }

    @Test
    fun `meeting more preferences beats being cheaper`() {
        val pick = ranker.pick(base.copy(cpuProfile = sustained(INFERRED), storage = localNvme(INFERRED)))!!

        assertEquals("storage.medium", pick.entry.instanceType) // dedicated.medium is cheaper but has no local disk
        assertEquals(2, pick.preferencesMet.size)
    }

    @Test
    fun `an inferred ARM trait allows ARM types`() {
        assertEquals("burst.medium.arm", ranker.pick(base.copy(architecture = arm(INFERRED)))!!.entry.instanceType)
    }
}
