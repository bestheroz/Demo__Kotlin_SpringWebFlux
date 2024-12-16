package com.github.bestheroz.standard.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.bestheroz.standard.common.enums.AuthorityEnumListReadConverter
import com.github.bestheroz.standard.common.enums.AuthorityEnumListWriteConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions

@Configuration
class R2dbcConfig(
    private val objectMapper: ObjectMapper,
) {
    @Bean
    fun r2dbcCustomConversions(): R2dbcCustomConversions =
        R2dbcCustomConversions(
            R2dbcCustomConversions.STORE_CONVERSIONS,
            listOf(
                AuthorityEnumListReadConverter(objectMapper),
                AuthorityEnumListWriteConverter(objectMapper),
            ),
        )
}
