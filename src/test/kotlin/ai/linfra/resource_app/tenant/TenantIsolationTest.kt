package ai.linfra.resource_app.tenant

import ai.linfra.resource_app.IntegrationTest
import ai.linfra.resource_app.intent.persistence.Intent
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import java.util.UUID

class TenantIsolationTest : IntegrationTest() {

    private val keyA = "key-a-${UUID.randomUUID()}"
    private val keyB = "key-b-${UUID.randomUUID()}"

    @Test
    fun `missing or unknown key is rejected`() {
        val intent = intents.save(Intent(tenantId = tenant(keyA).id, spec = mapOf()))

        mvc.get("/api/v1/intents/${intent.id}").andExpect { status { isUnauthorized() } }
        mvc.get("/api/v1/intents/${intent.id}") { header("Authorization", "Bearer wrong") }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `another tenant's intent is not found`() {
        val intent = intents.save(Intent(tenantId = tenant(keyA).id, spec = mapOf()))
        tenant(keyB)

        mvc.get("/api/v1/intents/${intent.id}") { header("Authorization", "Bearer $keyB") }.andExpect { status { isNotFound() } }
        mvc.get("/api/v1/intents/${intent.id}") { header("Authorization", "Bearer $keyA") }.andExpect { status { isOk() } }
    }
}
