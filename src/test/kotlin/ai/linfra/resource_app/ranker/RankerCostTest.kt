package ai.linfra.resource_app.ranker

import ai.linfra.resource_app.conversation.model.TraitSource.STATED
import ai.linfra.resource_app.ranker.RankerFixtures.base
import ai.linfra.resource_app.ranker.RankerFixtures.localNvme
import ai.linfra.resource_app.ranker.RankerFixtures.ranker
import ai.linfra.resource_app.ranker.model.Area
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Total monthly cost, not instance list price. */
class RankerCostTest {

    @Test
    fun `total adds network disk, egress and the public IPv4 to the instance price`() {
        val pick = ranker.pick(base)!!

        assertEquals(20, pick.diskGb)
        assertEquals("20.00".toBigDecimal(), pick.cost.instance)
        assertEquals("1.60".toBigDecimal(), pick.cost.disk)   // 20 GB x 0.08
        assertEquals("0.80".toBigDecimal(), pick.cost.egress) // 10 GB x 0.08
        assertEquals("3.00".toBigDecimal(), pick.cost.ipv4)
        assertEquals("25.40".toBigDecimal(), pick.cost.total)
    }

    @Test
    fun `a type's own local disk is not billed separately`() {
        assertEquals("0.00".toBigDecimal(), ranker.pick(base.copy(storage = localNvme(STATED)))!!.cost.disk)
    }

    @Test
    fun `high egress dominates the bill`() {
        val cost = ranker.pick(base.copy(estimatedEgressGbPerMonth = 5_000))!!.cost

        assertEquals("400.00".toBigDecimal(), cost.egress)
        assert(cost.egress > cost.instance * 10.toBigDecimal())
    }

    @Test
    fun `budget applies to the total, not the instance price`() {
        // burst.medium lists at 20.00 but totals 25.40
        assertNull(ranker.pick(base.copy(maxMonthlyEur = "22".toBigDecimal())))
    }

    @Test
    fun `regions are priced differently`() {
        assertEquals("18.00".toBigDecimal(), ranker.pick(base.copy(area = Area.US))!!.cost.instance)
    }
}
