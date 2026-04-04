package com.xeno.subpilot.chat.properties

import org.springframework.boot.context.properties.ConfigurationProperties

import java.time.Duration

@ConfigurationProperties(prefix = "chat.history")
data class ChatHistoryProperties(
    val maxMessages: Int,
    val ttl: Duration,
)
