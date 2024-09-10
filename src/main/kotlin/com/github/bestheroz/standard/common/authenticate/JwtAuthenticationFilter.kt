package com.github.bestheroz.standard.common.authenticate

import com.github.bestheroz.standard.common.log.logger
import com.github.bestheroz.standard.config.SecurityConfig
import org.apache.commons.lang3.StringUtils
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.springframework.web.util.pattern.PathPatternParser
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
) : WebFilter {
    companion object {
        private const val REQUEST_COMPLETE_EXECUTE_TIME = "{} ....... Request Complete Execute Time ....... : {} ms"
        private const val REQUEST_PARAMETERS = "<{}>{}?{}"
        private val log = logger()
    }

    private val publicGetPaths = SecurityConfig.GET_PUBLIC.map { PathPatternParser().parse(it) }
    private val publicPostPaths = SecurityConfig.POST_PUBLIC.map { PathPatternParser().parse(it) }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val requestPath = request.path.value()

        if (requestPath == "/") {
            exchange.response.statusCode = HttpStatus.NOT_FOUND
            return Mono.empty()
        }

        if (!requestPath.startsWith("/api/v1/health/")) {
            log.info(
                REQUEST_PARAMETERS,
                request.method,
                requestPath,
                StringUtils.defaultString(request.uri.query)
            )
        }

        val startTime = Instant.now()

        return isPublicPath(request)
            .flatMap { isPublic ->
                if (isPublic) {
                    chain.filter(exchange)
                } else {
                    authenticateRequest(exchange, chain)
                }
            }
            .doFinally {
                val duration = Duration.between(startTime, Instant.now()).toMillis()
                if (!requestPath.startsWith("/api/v1/health/")) {
                    log.info(REQUEST_COMPLETE_EXECUTE_TIME, requestPath, duration)
                }
            }
    }

    private fun authenticateRequest(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val token = jwtTokenProvider.resolveAccessToken(exchange.request)

        return if (token == null) {
            log.info("No access token found")
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            Mono.empty()
        } else if (!jwtTokenProvider.validateToken(token)) {
            log.info("Invalid access token - refresh token required")
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            Mono.empty()
        } else {
            jwtTokenProvider.getOperator(token)
                .map { userDetails ->
                    UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
                }
                .flatMap { auth ->
                    chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                }
        }
    }


    private fun isPublicPath(request: ServerHttpRequest): Mono<Boolean> {
        val path = request.path.pathWithinApplication()
        return Mono.just(
            when (request.method) {
                HttpMethod.GET -> publicGetPaths.any { it.matches(path) }
                HttpMethod.POST -> publicPostPaths.any { it.matches(path) }
                else -> false
            }
        )
    }
}
