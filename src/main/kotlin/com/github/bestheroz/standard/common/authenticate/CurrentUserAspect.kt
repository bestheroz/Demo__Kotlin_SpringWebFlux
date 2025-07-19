package com.github.bestheroz.standard.common.authenticate

import com.github.bestheroz.standard.common.exception.AuthenticationException401
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.log.logger
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Aspect
@Component
class CurrentUserAspect {
    companion object {
        private val log = logger()
    }

    @Around(
        "execution(* com.github.bestheroz..*(.., @com.github.bestheroz.standard.common.authenticate.CurrentUser (*), ..))",
    )
    fun checkCurrentUser(joinPoint: ProceedingJoinPoint): Mono<Any> {
        return ReactiveSecurityContextHolder
            .getContext()
            .switchIfEmpty(
                Mono.error(
                    AuthenticationException401(ExceptionCode.EXPIRED_TOKEN).also {
                        log.error(
                            "@CurrentUser 인증 컨텍스트 누락 - Authentication context missing for method: ${joinPoint.signature.name}",
                        )
                    },
                ),
            ).flatMap { securityContext ->
                val authentication = securityContext.authentication
                if (
                    authentication == null ||
                    !authentication.isAuthenticated ||
                    authentication.principal == null
                ) {
                    log.error(
                        "@CurrentUser 인증 정보 누락 - Authentication missing or invalid for method: ${joinPoint.signature.name}",
                    )
                    return@flatMap Mono.error<Any>(AuthenticationException401(ExceptionCode.EXPIRED_TOKEN))
                }
                // Proceed with the joinPoint
                Mono.fromCallable { joinPoint.proceed() }
            }
    }
}
