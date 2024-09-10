package com.github.bestheroz.standard.common.exception

import com.github.bestheroz.standard.common.log.logger
import com.github.bestheroz.standard.common.response.ApiResult
import com.github.bestheroz.standard.common.response.Result
import com.github.bestheroz.standard.common.util.LogUtils
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.MethodNotAllowedException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebInputException
import org.springframework.web.server.UnsupportedMediaTypeStatusException
import reactor.core.publisher.Mono
import java.lang.IllegalStateException

@RestControllerAdvice
class ApiExceptionHandler {
    companion object {
        private val log = logger()
    }

    @ExceptionHandler(Throwable::class)
    fun exception(e: Throwable): Mono<ResponseEntity<ApiResult<*>>> {
        log.error(LogUtils.getStackTrace(e))
        return Mono.just(Result.error())
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun responseStatusException(e: ResponseStatusException): Mono<ResponseEntity<ApiResult<*>>> {
        log.error(LogUtils.getStackTrace(e))
        return Mono.just(ResponseEntity.status(e.statusCode).build<ApiResult<*>>())
    }

    @ExceptionHandler(RequestException400::class)
    fun requestException400(e: RequestException400): Mono<ResponseEntity<ApiResult<*>>> {
        log.warn(LogUtils.getStackTrace(e))
        return Mono.just(ResponseEntity.badRequest().body(ApiResult.of(e.exceptionCode, e.data)))
    }

    @ExceptionHandler(AuthenticationException401::class)
    fun authenticationException401(e: AuthenticationException401): Mono<ResponseEntity<ApiResult<*>>> {
        log.warn(LogUtils.getStackTrace(e))
        val builder = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        if (e.exceptionCode == ExceptionCode.EXPIRED_TOKEN) {
            builder.header("token", "must-renew")
        }
        return Mono.just(builder.body(ApiResult.of(e.exceptionCode, e.data)))
    }

    @ExceptionHandler(AuthorityException403::class)
    fun authorityException403(e: AuthorityException403): Mono<ResponseEntity<ApiResult<*>>> {
        log.warn(LogUtils.getStackTrace(e))
        return Mono.just(ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResult.of(e.exceptionCode, e.data)))
    }

    @ExceptionHandler(AuthorizationDeniedException::class, AccessDeniedException::class)
    fun authorizationDeniedException(e: AccessDeniedException): Mono<ResponseEntity<ApiResult<*>>> {
        log.warn(LogUtils.getStackTrace(e))
        return Mono.just(ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResult.of(ExceptionCode.UNKNOWN_AUTHORITY)))
    }

    @ExceptionHandler(SystemException500::class)
    fun systemException500(e: SystemException500): Mono<ResponseEntity<ApiResult<*>>> {
        log.warn(LogUtils.getStackTrace(e))
        return Mono.just(ResponseEntity
            .internalServerError()
            .body(ApiResult.of(e.exceptionCode, e.data)))
    }

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun illegalArgumentException(e: IllegalArgumentException): Mono<ResponseEntity<ApiResult<*>>> {
        log.warn(LogUtils.getStackTrace(e))
        return Mono.just(ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiResult.of(ExceptionCode.INVALID_PARAMETER)))
    }

    @ExceptionHandler(UsernameNotFoundException::class)
    fun usernameNotFoundException(e: UsernameNotFoundException): Mono<ResponseEntity<ApiResult<*>>> =
        Mono.just(Result.unauthenticated())

    @ExceptionHandler(ServerWebInputException::class)
    fun serverWebInputException(e: ServerWebInputException): Mono<ResponseEntity<ApiResult<*>>> {
        log.warn(LogUtils.getStackTrace(e))
        return Mono.just(ResponseEntity.badRequest().build())
    }

    @ExceptionHandler(
        UnsupportedMediaTypeStatusException::class,
        MethodNotAllowedException::class
    )
    fun webFluxExceptions(e: Throwable): Mono<ResponseEntity<ApiResult<*>>> {
        log.warn(LogUtils.getStackTrace(e))
        return Mono.just(ResponseEntity.badRequest().build())
    }

    @ExceptionHandler(DuplicateKeyException::class)
    fun duplicateKeyException(e: DuplicateKeyException): Mono<ResponseEntity<ApiResult<*>>> {
        log.warn(LogUtils.getStackTrace(e))
        return Mono.just(ResponseEntity.badRequest().build())
    }
}
