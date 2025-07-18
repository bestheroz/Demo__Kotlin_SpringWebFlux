package com.github.bestheroz.standard.config

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.converter.ConverterFactory
import org.springframework.data.convert.CustomConversions.StoreConversions
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@Configuration
@EnableR2dbcRepositories
class R2dbcConfig(
    private val objectMapper: ObjectMapper,
) {
    @Bean
    fun r2dbcCustomConversions(): R2dbcCustomConversions =
        R2dbcCustomConversions(
            StoreConversions.NONE,
            listOf(
                StringToEnumConverterFactory(), // ConverterFactory 로 변경하여 우선순위를 높임
                EnumToStringConverter(),
                StringToEnumListConverterFactory(), // ConverterFactory 로 변경하여 우선순위를 높임
                EnumListToStringConverter(objectMapper),
                ByteToBooleanConverter(),
                BooleanToByteConverter(),
                MapReadConverter(objectMapper),
                MapWriteConverter(objectMapper),
            ),
        )
}

@ReadingConverter
class ByteToBooleanConverter : Converter<Byte, Boolean> {
    override fun convert(source: Byte): Boolean = source.toInt() != 0
}

@WritingConverter
class BooleanToByteConverter : Converter<Boolean, Byte> {
    override fun convert(source: Boolean): Byte = if (source) 1.toByte() else 0.toByte()
}

@ReadingConverter
class StringToEnumConverterFactory : ConverterFactory<String, Enum<*>> {
    override fun <T : Enum<*>> getConverter(targetType: Class<T>): Converter<String, T> = StringToEnumConverter(targetType.enumConstants as Array<T>)
}

private class StringToEnumConverter<T : Enum<*>>(
    private val values: Array<T>,
) : Converter<String, T> {
    override fun convert(source: String): T? {
        if (source.isBlank()) return null

        return try {
            val trimmed = source.trim().replace("\"", "")
            when {
                trimmed.isEmpty() -> null
                trimmed == "null" -> null
                else -> values.firstOrNull { it.name == trimmed }
            }
        } catch (e: Exception) {
            null
        }
    }
}

@WritingConverter
class EnumToStringConverter : Converter<Enum<*>, String> {
    override fun convert(source: Enum<*>): String =
        try {
            source.name
        } catch (e: Exception) {
            ""
        }
}

@ReadingConverter
class StringToEnumListConverterFactory : ConverterFactory<String, List<Enum<*>>> {
    override fun <T : List<Enum<*>>> getConverter(targetType: Class<T>): Converter<String, T> = StringToEnumListConverter() as Converter<String, T>

    private class StringToEnumListConverter : Converter<String, List<Enum<*>>> {
        override fun convert(source: String): List<Enum<*>> {
            if (source.isBlank()) return emptyList()

            return try {
                val trimmed = source.trim()
                when {
                    trimmed.isEmpty() -> emptyList()
                    trimmed == "[]" -> emptyList()
                    trimmed == "null" -> emptyList()
                    trimmed.startsWith("[") && trimmed.endsWith("]") -> {
                        val content = trimmed.substring(1, trimmed.length - 1).trim()
                        if (content.isEmpty()) {
                            emptyList()
                        } else {
                            content
                                .split(",")
                                .map { it.trim().replace("\"", "") }
                                .filter { it.isNotEmpty() }
                                .mapNotNull { enumValue ->
                                    // 여기서 실제 Enum 타입을 찾아서 변환
                                    val enumClass =
                                        Class.forName(
                                            Thread
                                                .currentThread()
                                                .contextClassLoader
                                                .loadClass(enumValue)
                                                .name,
                                        ) as Class<out Enum<*>>
                                    java.lang.Enum.valueOf(enumClass as Class<out Enum<*>>, enumValue)
                                }
                        }
                    }
                    else -> {
                        val enumClass =
                            Class.forName(
                                Thread
                                    .currentThread()
                                    .contextClassLoader
                                    .loadClass(trimmed)
                                    .name,
                            )
                                as Class<out Enum<*>>
                        listOf(java.lang.Enum.valueOf(enumClass, trimmed))
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

@WritingConverter
class EnumListToStringConverter(
    private val objectMapper: ObjectMapper,
) : Converter<List<Enum<*>>, String> {
    override fun convert(source: List<Enum<*>>): String =
        try {
            if (source.isEmpty()) {
                "[]"
            } else {
                objectMapper.writeValueAsString(source.map { it.name })
            }
        } catch (e: Exception) {
            "[]"
        }
}

@ReadingConverter
class MapReadConverter(
    private val objectMapper: ObjectMapper,
) : Converter<String, Map<String, Any>> {
    override fun convert(source: String): Map<String, Any> = objectMapper.readValue(source, object : TypeReference<Map<String, Any>>() {})
}

@WritingConverter
class MapWriteConverter(
    private val objectMapper: ObjectMapper,
) : Converter<Map<String, Any>, String> {
    override fun convert(source: Map<String, Any>): String = objectMapper.writeValueAsString(source)
}
