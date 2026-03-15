package com.github.bestheroz.standard.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.WebFluxConfigurer

// springdoc 3.x는 swagger-ui 리소스를 자체 auto-configuration으로 서빙하므로
// 커스텀 리소스 핸들러 등록이 불필요
@Configuration
class WebConfig : WebFluxConfigurer
