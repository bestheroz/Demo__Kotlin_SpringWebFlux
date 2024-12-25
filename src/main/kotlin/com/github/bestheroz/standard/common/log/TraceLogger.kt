package com.github.bestheroz.standard.common.log

import org.apache.commons.lang3.StringUtils
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Aspect
@Component
class TraceLogger {
    companion object {
        private val log = logger()
        private const val STR_START_EXECUTE_TIME = "{} START ......."
        private const val STR_END_EXECUTE_TIME = "{} E N D [{}ms]"
        private const val STR_END_EXECUTE_TIME_FOR_EXCEPTION = "{} THROW [{}ms]"
    }

    @Around(
        "execution(!private * com.github.bestheroz..*Controller.*(..)) || " +
            "execution(!private * com.github.bestheroz..*Service.*(..)) || " +
            "execution(!private * com.github.bestheroz..*Repository.*(..))",
    )
    @Throws(Throwable::class)
    fun writeLog(pjp: ProceedingJoinPoint): Any? {
        val signature =
            pjp.staticPart.signature
                .toString()
                .removePrefix(pjp.staticPart.signature.declaringType.`package`.name + ".")

        if (signature.containsAny("HealthController", "HealthRepository")) {
            return pjp.proceed()
        }

        val stopWatch = StopWatch(signature)
        stopWatch.start()
        log.info(STR_START_EXECUTE_TIME, signature)

        return try {
            val result = pjp.proceed()

            when (result) {
                is Mono<*> -> {
                    result
                        .doOnSuccess {
                            stopWatch.stop()
                            log.info(STR_END_EXECUTE_TIME, signature, stopWatch.totalTimeMillis)
                        }.doOnError {
                            stopWatch.stop()
                            log.info(STR_END_EXECUTE_TIME_FOR_EXCEPTION, signature, stopWatch.totalTimeMillis)
                        }
                }
                is Flux<*> -> {
                    result
                        .doOnComplete {
                            stopWatch.stop()
                            log.info(STR_END_EXECUTE_TIME, signature, stopWatch.totalTimeMillis)
                        }.doOnError {
                            stopWatch.stop()
                            log.info(STR_END_EXECUTE_TIME_FOR_EXCEPTION, signature, stopWatch.totalTimeMillis)
                        }
                }
                else -> {
                    stopWatch.stop()
                    log.info(STR_END_EXECUTE_TIME, signature, stopWatch.totalTimeMillis)
                    result
                }
            }
        } catch (e: Throwable) {
            stopWatch.stop()
            log.info(STR_END_EXECUTE_TIME_FOR_EXCEPTION, signature, stopWatch.totalTimeMillis)
            throw e
        }
    }

    private fun String.containsAny(vararg substrings: String): Boolean = StringUtils.containsAny(this, *substrings)
}
