package ai.linfra.resource_app.common.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AntPathRequestMatcher


@Configuration
@EnableWebSecurity
class SecurityFilter {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf().disable().authorizeHttpRequests { config ->
            try {
                config
                    .requestMatchers(AntPathRequestMatcher("/**", "OPTIONS"))
                    .permitAll()
                    .requestMatchers("/api/v1/notifications/resources/**")
                    .hasRole("RESOURCE_ADMIN")
                    .and()
                    .httpBasic()
                    .and()
                    .authorizeHttpRequests()
                    .requestMatchers("/**")
                    .authenticated()
                    .and()
                    .oauth2ResourceServer()
                    .jwt()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        return http.build()
    }


    @Bean
    fun users(): UserDetailsService {
        val userBuilder = User.withDefaultPasswordEncoder()
        val user = userBuilder
            .username("user")
            .password("password")
            .roles("RESOURCE_ADMIN")
            .build()

        return InMemoryUserDetailsManager(user)
    }
}
