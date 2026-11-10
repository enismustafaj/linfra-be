package ai.linfra.resource_app.tenant.security

import ai.linfra.resource_app.tenant.model.TenantContext
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/** Injects the TenantContext that ApiKeyFilter put on the request. */
class TenantArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(p: MethodParameter) = p.parameterType == TenantContext::class.java

    override fun resolveArgument(
        p: MethodParameter, mav: ModelAndViewContainer?, req: NativeWebRequest, binder: WebDataBinderFactory?,
    ) = req.getAttribute(TENANT_ATTR, RequestAttributes.SCOPE_REQUEST)
        ?: error("TenantContext requested outside /api/*: route is not behind ApiKeyFilter")
}
