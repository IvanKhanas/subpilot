package com.xeno.subpilot.chat.config

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.xeno.subpilot.chat.properties.ChatHistoryProperties
import com.xeno.subpilot.chat.properties.OpenAiProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(OpenAiProperties::class, ChatHistoryProperties::class)
class OpenAiConfig {

    @Bean
    fun openAiClient(properties: OpenAiProperties): OpenAIClient =
        OpenAIOkHttpClient
            .builder()
            .apiKey(properties.apiKey)
            .build()
}
