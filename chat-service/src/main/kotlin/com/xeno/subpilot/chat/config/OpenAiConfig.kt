package com.xeno.subpilot.chat.config

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.xeno.subpilot.chat.properties.OpenAiProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenAiConfig {

    @Bean
    fun openAiClient(properties: OpenAiProperties): OpenAIClient =
        OpenAIOkHttpClient
            .builder()
            .apiKey(properties.apiKey)
            .build()
}
