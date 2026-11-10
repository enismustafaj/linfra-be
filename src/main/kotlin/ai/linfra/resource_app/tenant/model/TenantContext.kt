package ai.linfra.resource_app.tenant.model

import java.util.UUID

/** Authenticated caller. Controllers take this as a parameter; the tenant id never comes from the request body. */
data class TenantContext(val tenantId: UUID)
