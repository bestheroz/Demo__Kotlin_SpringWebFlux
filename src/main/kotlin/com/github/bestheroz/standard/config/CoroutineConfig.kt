package com.github.bestheroz.standard.config

import com.github.bestheroz.standard.common.util.LogUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.beans.factory.DisposableBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoroutineConfig : DisposableBean {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val job = SupervisorJob()
    private val exceptionHandler =
        CoroutineExceptionHandler { _, exception ->
            logger.error { LogUtils.getStackTrace(exception) }
        }

    // kotlinx-coroutines 1.10.x + Java 25: Dispatchers.IO가 자동으로 virtual thread 기반으로 동작
    private val coroutineScope = CoroutineScope(job + Dispatchers.IO + exceptionHandler)

    @Bean fun coroutineScope(): CoroutineScope = coroutineScope

    override fun destroy() {
        job.cancel() // 스코프 취소
    }
}
