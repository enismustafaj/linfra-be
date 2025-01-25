package ai.linfra.resource_app.common.security

import ai.linfra.resource_app.common.security.config.JwtConfiguration
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AntPathRequestMatcher


@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtConfiguration::class)
class SecurityFilter(
    val jwtConfiguration: JwtConfiguration
) {

    @Bean
    @Order(1)
    fun authServerFilterChain(http: HttpSecurity): SecurityFilterChain {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http)
        http.getConfigurer(OAuth2AuthorizationServerConfigurer::class.java)
            .oidc(Customizer.withDefaults())

        return http.build()
    }

    @Bean
    fun jwkSource(): JWKSource<SecurityContext> {
        val jwkSet = JWKSet.parse(jwtConfiguration.jwks)
        val signingKeySet = JWKSet(jwkSet.keys[0])

        return JWKSource { jwkSelector, _ -> jwkSelector.select(signingKeySet) }
    }

    @Bean
    @Order(2)
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { csrf ->
            csrf.disable()
        }.authorizeHttpRequests { config ->
            try {
                config
                    .requestMatchers(AntPathRequestMatcher("/**", "OPTIONS"))
                    .permitAll()
                    .requestMatchers("/**")
                    .authenticated()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }.oauth2ResourceServer { oauth2 ->
            oauth2.authenticationManagerResolver(JwtIssuerAuthenticationManagerResolver
                .fromTrustedIssuers("https://securetoken.google.com/linfra-ai", "http://localhost:8080"))
        }

        return http.build()
    }
}
