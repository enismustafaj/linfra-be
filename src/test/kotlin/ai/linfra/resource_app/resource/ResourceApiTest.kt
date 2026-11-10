package ai.linfra.resource_app.resource

import ai.linfra.resource_app.IntegrationTest
import ai.linfra.resource_app.ranker.model.Provider
import ai.linfra.resource_app.resource.persistence.Resource
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import java.util.UUID

class ResourceApiTest : IntegrationTest() {

    private fun resourceFor(key: String): Resource {
        val intent = plannedIntent(tenant(key).id)
        return resources.save(
            Resource(
                tenantId = intent.tenantId, intentId = intent.id, provider = Provider.DOCKER, region = "local-eu",
                instanceType = "burst.medium", cloudId = "c-${UUID.randomUUID()}", outputs = mapOf("ipv4" to "172.17.0.9"),
            )
        )
    }

    @Test
    fun `a tenant lists and reads its own resources`() {
        val key = "key-${UUID.randomUUID()}"
        val resource = resourceFor(key)

        mvc.get("/api/v1/resources") { header("Authorization", "Bearer $key") }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].cloudId") { value(resource.cloudId) }
        }
        mvc.get("/api/v1/resources/${resource.id}") { header("Authorization", "Bearer $key") }.andExpect {
            status { isOk() }
            jsonPath("$.intentId") { value(resource.intentId.toString()) }
            jsonPath("$.outputs.ipv4") { value("172.17.0.9") }
            jsonPath("$.status") { value("ACTIVE") }
        }
    }

    @Test
    fun `another tenant's resource is not listed and not found`() {
        val resource = resourceFor("key-${UUID.randomUUID()}")
        val other = "other-${UUID.randomUUID()}"
        tenant(other)

        mvc.get("/api/v1/resources") { header("Authorization", "Bearer $other") }.andExpect { jsonPath("$.length()") { value(0) } }
        mvc.get("/api/v1/resources/${resource.id}") { header("Authorization", "Bearer $other") }.andExpect { status { isNotFound() } }
    }
}
