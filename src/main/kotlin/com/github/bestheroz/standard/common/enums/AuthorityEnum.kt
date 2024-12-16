package com.github.bestheroz.standard.common.enums

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

enum class AuthorityEnum(
    private val value: String,
) {
    ADMIN_VIEW("ADMIN_VIEW"),
    ADMIN_EDIT("ADMIN_EDIT"),
    USER_VIEW("USER_VIEW"),
    USER_EDIT("USER_EDIT"),
    NOTICE_VIEW("NOTICE_VIEW"),
    NOTICE_EDIT("NOTICE_EDIT"),
}

@ReadingConverter
class AuthorityEnumListReadConverter(
    private val objectMapper: ObjectMapper,
) : Converter<String, List<AuthorityEnum>> {
    override fun convert(source: String): List<AuthorityEnum> {
        if (source.isEmpty() || source == "[]") return emptyList()
        return objectMapper.readValue(source, object : TypeReference<List<AuthorityEnum>>() {})
    }
}

@WritingConverter
class AuthorityEnumListWriteConverter(
    private val objectMapper: ObjectMapper,
) : Converter<List<AuthorityEnum>, String> {
    override fun convert(source: List<AuthorityEnum>): String = objectMapper.writeValueAsString(source)
}
