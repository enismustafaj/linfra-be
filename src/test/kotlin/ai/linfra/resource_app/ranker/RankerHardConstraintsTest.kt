package ai.linfra.resource_app.ranker

import ai.linfra.resource_app.conversation.model.TraitSource.STATED
import ai.linfra.resource_app.ranker.RankerFixtures.arm
import ai.linfra.resource_app.ranker.RankerFixtures.base
import ai.linfra.resource_app.ranker.RankerFixtures.disk
import ai.linfra.resource_app.ranker.RankerFixtures.localNvme
import ai.linfra.resource_app.ranker.RankerFixtures.ranker
import ai.linfra.resource_app.ranker.RankerFixtures.sustained
import ai.linfra.resource_app.ranker.model.Architecture
import ai.linfra.resource_app.ranker.model.Area
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** STATED traits, size, area and budget exclude candidates. */
class RankerHardConstraintsTest {

    @Test
    fun `without an architecture trait ARM is never offered, even when cheaper`() {
        val pick = ranker.pick(base)!!

        assertEquals("burst.medium", pick.entry.instanceType) // burst.medium.arm is cheaper but would break x86 images
        assertEquals(Architecture.X86, pick.entry.architecture)
    }

    @Test
    fun `stated ARM picks an ARM type`() {
        assertEquals("burst.medium.arm", ranker.pick(base.copy(architecture = arm(STATED)))!!.entry.instanceType)
    }

    @Test
    fun `stated local disk picks a type with its own disk`() {
        assertEquals("storage.medium", ranker.pick(base.copy(storage = localNvme(STATED)))!!.entry.instanceType)
    }

    @Test
    fun `stated local disk that is too small for a stated size means nothing fits`() {
        assertNull(ranker.pick(base.copy(storage = localNvme(STATED), disk = disk(200, STATED))))
    }

    @Test
    fun `a stated network disk size is provisioned as asked`() {
        val pick = ranker.pick(base.copy(disk = disk(200, STATED)))!!

        assertEquals("burst.medium", pick.entry.instanceType)
        assertEquals(200, pick.diskGb)
    }

    @Test
    fun `stated sustained CPU that does not fit the budget means nothing fits`() {
        assertNull(ranker.pick(base.copy(cpuProfile = sustained(STATED), maxMonthlyEur = "40".toBigDecimal())))
    }

    @Test
    fun `area is respected`() {
        assertEquals("local-us", ranker.pick(base.copy(area = Area.US))!!.entry.region)
    }
}
