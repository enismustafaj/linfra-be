package ai.linfra.resource_app.tenant.security

import ai.linfra.resource_app.tenant.model.TenantContext
import ai.linfra.resource_app.tenant.persistence.TenantRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

const val TENANT_ATTR = "linfra.tenant"

class ApiKeyFilter(private val tenants: TenantRepository) : OncePerRequestFilter() {
    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val key = req.getHeader("Authorization")?.removePrefix("Bearer ")?.trim()
        val tenant = key?.takeIf { it.isNotEmpty() }?.let { tenants.findByApiKeyHash(hashApiKey(it)) }
        if (tenant == null) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            return
        }
        req.setAttribute(TENANT_ATTR, TenantContext(tenant.id))
        chain.doFilter(req, res)
    }
}
