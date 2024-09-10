package com.github.bestheroz.standard.common.authenticate

import com.github.bestheroz.standard.common.exception.AuthenticationException401
import com.github.bestheroz.standard.common.exception.ExceptionCode
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Aspect
@Component
class CurrentUserAspect {
    @Around("execution(* com.github.bestheroz..*(.., @com.github.bestheroz.standard.common.authenticate.CurrentUser (*), ..))")
    fun checkCurrentUser(joinPoint: ProceedingJoinPoint): Mono<Any> {
        return ReactiveSecurityContextHolder.getContext()
            .switchIfEmpty(Mono.error(AuthenticationException401(ExceptionCode.EXPIRED_TOKEN)))
            .flatMap { securityContext ->
                val authentication = securityContext.authentication
                if (authentication == null || !authentication.isAuthenticated || authentication.principal == null) {
                    return@flatMap Mono.error<Any>(AuthenticationException401(ExceptionCode.EXPIRED_TOKEN))
                }
                // Proceed with the joinPoint
                Mono.fromCallable { joinPoint.proceed() }
            }
    }
}
