package ai.linfra.resource_app.tenant.security

import ai.linfra.resource_app.tenant.persistence.TenantRepository
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class ApiKeyAuthConfig : WebMvcConfigurer {

    @Bean
    fun apiKeyFilter(tenants: TenantRepository) = FilterRegistrationBean(ApiKeyFilter(tenants)).apply {
        addUrlPatterns("/api/*")
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(TenantArgumentResolver())
    }
}
