package ai.linfra.resource_app.remediation

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID

class FindingsApiTest : RemediationTest() {

    private val keyA = "a-${UUID.randomUUID()}"
    private val keyB = "b-${UUID.randomUUID()}"

    @Test
    fun `a tenant lists its open findings with the actions it can take`() {
        val intent = plannedIntent(tenant(keyA).id)
        reconcileWith(observed("c-1", intent))

        mvc.get("/api/v1/findings?status=OPEN") { header("Authorization", "Bearer $keyA") }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].type") { value("ORPHAN") }
            jsonPath("$[0].cloudIds[0]") { value("c-1") }
            jsonPath("$[0].availableActions.length()") { value(2) }
            jsonPath("$[0].availableActions[0]") { value("DESTROY") }
            jsonPath("$[0].availableActions[1]") { value("DISMISS") }
        }
    }

    @Test
    fun `taking an action resolves the finding and an unavailable one is a conflict`() {
        val intent = plannedIntent(tenant(keyA).id)
        reconcileWith(observed("c-1", intent))
        val id = open(ai.linfra.resource_app.remediation.model.FindingType.ORPHAN).id

        mvc.post("/api/v1/findings/$id/actions") {
            header("Authorization", "Bearer $keyA"); contentType = MediaType.APPLICATION_JSON; content = """{"action":"KEEP","cloudId":"c-1"}"""
        }.andExpect { status { isConflict() } }

        mvc.post("/api/v1/findings/$id/actions") {
            header("Authorization", "Bearer $keyA"); contentType = MediaType.APPLICATION_JSON; content = """{"action":"DESTROY"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("RESOLVED") }
            jsonPath("$.resolution") { value("DESTROY") }
            jsonPath("$.availableActions.length()") { value(0) }
        }
    }

    @Test
    fun `another tenant can't see or act on the finding`() {
        val intent = plannedIntent(tenant(keyA).id)
        tenant(keyB)
        reconcileWith(observed("c-1", intent))
        val id = open(ai.linfra.resource_app.remediation.model.FindingType.ORPHAN).id

        mvc.get("/api/v1/findings") { header("Authorization", "Bearer $keyB") }.andExpect { jsonPath("$.length()") { value(0) } }
        mvc.get("/api/v1/findings/$id") { header("Authorization", "Bearer $keyB") }.andExpect { status { isNotFound() } }
        mvc.post("/api/v1/findings/$id/actions") {
            header("Authorization", "Bearer $keyB"); contentType = MediaType.APPLICATION_JSON; content = """{"action":"DESTROY"}"""
        }.andExpect { status { isNotFound() } }
    }
}
