package com.github.bestheroz.standard.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.web.reactive.config.ResourceHandlerRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
class WebConfig(
    private val objectMapper: ObjectMapper,
) : WebFluxConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry
            .addResourceHandler("/swagger-ui/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/springdoc-openapi-ui/")
            .setCacheControl(CacheControl.noCache())
    }

    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        configurer
            .defaultCodecs()
            .jackson2JsonEncoder(
                org.springframework.http.codec.json
                    .Jackson2JsonEncoder(objectMapper),
            )
        configurer
            .defaultCodecs()
            .jackson2JsonDecoder(
                org.springframework.http.codec.json
                    .Jackson2JsonDecoder(objectMapper),
            )
    }
}
