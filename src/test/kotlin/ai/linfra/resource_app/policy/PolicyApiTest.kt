package ai.linfra.resource_app.policy

import ai.linfra.resource_app.intent.PlanningTest
import ai.linfra.resource_app.intent.model.IntentState
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import java.util.UUID
import kotlin.test.assertEquals

class PolicyApiTest : PlanningTest() {

    private val key = "k-${UUID.randomUUID()}"

    private fun putPolicy(json: String, k: String = key) = mvc.put("/api/v1/policy") {
        header("Authorization", "Bearer $k"); contentType = MediaType.APPLICATION_JSON; content = json
    }

    private val valid = """
        {"allowedInstanceTypes":["burst.medium"],"allowedRegions":["local-eu"],
         "monthlyBudgetEur":100,"autoApproveMaxEur":20,"requireApprovalForProd":true}
    """

    @Test
    fun `a tenant without a policy sees the defaults and the catalog to choose from`() {
        tenant(key)

        mvc.get("/api/v1/policy") { header("Authorization", "Bearer $key") }.andExpect {
            status { isOk() }
            jsonPath("$.isDefault") { value(true) }
            jsonPath("$.monthlyBudgetEur") { value(200) }
            jsonPath("$.committedSpendEur") { value(0) }
            jsonPath("$.catalogInstanceTypes") { value(hasItems("burst.medium", "dedicated.large")) }
            jsonPath("$.catalogRegions") { value(hasItems("local-eu", "local-us")) }
        }
    }

    @Test
    fun `a saved policy is returned and governs the next decision`() {
        val tenantId = tenant(key).id

        putPolicy(valid).andExpect {
            status { isOk() }
            jsonPath("$.isDefault") { value(false) }
            jsonPath("$.allowedInstanceTypes[0]") { value("burst.medium") }
            jsonPath("$.autoApproveMaxEur") { value(20) }
        }

        assertEquals(IntentState.PLANNED, plan(proposed(tenantId)).state) // €25.40 > the new €20 limit
    }

    @Test
    fun `committed spend counts approved intents`() {
        val tenantId = tenant(key).id
        plan(proposed(tenantId)) // auto-approved under the defaults

        mvc.get("/api/v1/policy") { header("Authorization", "Bearer $key") }.andExpect { jsonPath("$.committedSpendEur") { value(25.40) } }
    }

    @Test
    fun `unknown catalog values and inconsistent limits are rejected`() {
        tenant(key)

        putPolicy(valid.replace("burst.medium", "huge.vm")).andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value(containsString("huge.vm")) }
        }
        putPolicy(valid.replace("\"autoApproveMaxEur\":20", "\"autoApproveMaxEur\":500")).andExpect { status { isBadRequest() } }
    }

    @Test
    fun `one tenant's policy doesn't change another's`() {
        tenant(key)
        val other = "o-${UUID.randomUUID()}"
        tenant(other)

        putPolicy(valid).andExpect { status { isOk() } }

        mvc.get("/api/v1/policy") { header("Authorization", "Bearer $other") }.andExpect { jsonPath("$.isDefault") { value(true) } }
    }
}
