package com.github.bestheroz.standard.config

import com.github.bestheroz.standard.common.authenticate.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {
    @Bean
    fun securityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain = http
        .csrf { it.disable() }
        .cors { it.configurationSource(corsConfigurationSource()) }
        .authorizeExchange {
            it.pathMatchers(HttpMethod.GET, *GET_PUBLIC).permitAll()
            it.pathMatchers(HttpMethod.POST, *POST_PUBLIC).permitAll()
            it.anyExchange().authenticated()
        }
        .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
        .build()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf("http://localhost:8081")
            allowedHeaders = listOf("*")
            allowedMethods = listOf("*")
            allowCredentials = true
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    companion object {
        val GET_PUBLIC = arrayOf(
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**",
            "/favicon.ico",
            "/api/v1/health/**",
            "/api/v1/notices",
            "/api/v1/notices/{id}",
            "/api/v1/admins/check-login-id",
            "/api/v1/admins/renew-token",
            "/api/v1/users/check-login-id",
            "/api/v1/users/renew-token"
        )
        val POST_PUBLIC = arrayOf(
            "/api/v1/admins/login",
            "/api/v1/users/login"
        )
    }
}
