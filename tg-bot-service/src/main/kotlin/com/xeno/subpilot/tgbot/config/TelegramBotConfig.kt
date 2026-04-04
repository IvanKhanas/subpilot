package com.xeno.subpilot.tgbot.config

import com.xeno.subpilot.tgbot.properties.NavigationProperties
import com.xeno.subpilot.tgbot.properties.TelegramBotProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper

import java.net.http.HttpClient
import java.time.Duration

private val logger = KotlinLogging.logger {}

@Configuration
@EnableConfigurationProperties(TelegramBotProperties::class, NavigationProperties::class)
class TelegramBotConfig {

    @Bean
    fun telegramRestClient(
        properties: TelegramBotProperties,
        jsonMapper: JsonMapper,
    ): RestClient =
        RestClient
            .builder()
            .requestFactory(
                JdkClientHttpRequestFactory(HttpClient.newHttpClient()).apply {
                    setReadTimeout(Duration.ofSeconds((properties.pollingTimeout + 5).toLong()))
                },
            ).baseUrl("${properties.baseUrl}/bot${properties.token}")
            .requestInterceptor(TokenMaskingInterceptor(properties.token))
            .messageConverters(
                listOf(
                    ByteArrayHttpMessageConverter(),
                    StringHttpMessageConverter(),
                    JacksonJsonHttpMessageConverter(jsonMapper),
                ),
            ).build()
}

private class TokenMaskingInterceptor(
    private val token: String,
) : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        logger.debug { "HTTP ${request.method} ${request.uri.toString().replace(token, "***")}" }
        return execution.execute(request, body)
    }
}
