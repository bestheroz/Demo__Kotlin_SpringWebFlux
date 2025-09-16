package com.github.bestheroz.standard.common.log

import io.github.oshai.kotlinlogging.KotlinLogging
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
        private val logger = KotlinLogging.logger {}
    }

    @Around(
        """
        execution(!private * com.github.bestheroz..*Controller.*(..)) ||
        execution(!private * com.github.bestheroz..*Service.*(..)) ||
        execution(!private * com.github.bestheroz..*Repository.*(..))
        """,
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
        logger.info { "$signature START ......." }

        return try {
            val result = pjp.proceed()

            when (result) {
                is Mono<*> -> {
                    result
                        .doOnSuccess {
                            stopWatch.stop()
                            logger.info { "$signature E N D [${stopWatch.totalTimeMillis}ms]" }
                        }.doOnError {
                            stopWatch.stop()
                            logger.info { "$signature THROW [${stopWatch.totalTimeMillis}ms]" }
                        }
                }
                is Flux<*> -> {
                    result
                        .doOnComplete {
                            stopWatch.stop()
                            logger.info { "$signature E N D [${stopWatch.totalTimeMillis}ms]" }
                        }.doOnError {
                            stopWatch.stop()
                            logger.info { "$signature THROW [${stopWatch.totalTimeMillis}ms]" }
                        }
                }
                else -> {
                    stopWatch.stop()
                    logger.info { "$signature E N D [${stopWatch.totalTimeMillis}ms]" }
                    result
                }
            }
        } catch (e: Throwable) {
            if (stopWatch.isRunning) {
                stopWatch.stop()
            }
            logger.info { "$signature THROW [${stopWatch.totalTimeMillis}ms]" }
            throw e
        }
    }

    private fun String.containsAny(vararg substrings: String): Boolean = StringUtils.containsAny(this, *substrings)
}
