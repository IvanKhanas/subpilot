package com.xeno.subpilot.tgbot.config

import com.xeno.subpilot.tgbot.properties.TelegramBotProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper

@Configuration
@EnableConfigurationProperties(TelegramBotProperties::class)
class TelegramBotConfig {

    @Bean
    fun telegramRestClient(
        properties: TelegramBotProperties,
        jsonMapper: JsonMapper,
    ): RestClient =
        RestClient
            .builder()
            .requestFactory(SimpleClientHttpRequestFactory())
            .baseUrl("${properties.baseUrl}/bot${properties.token}")
            .messageConverters { converters ->
                converters.removeIf { it is JacksonJsonHttpMessageConverter }
                converters.add(JacksonJsonHttpMessageConverter(jsonMapper))
            }.build()
}
