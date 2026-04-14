package com.xeno.subpilot.tgbot.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "grpc.retry")
data class GrpcRetryProperties(
    val maxAttempts: Int = 3,
    val initialBackoffMs: Long = 200,
    val backoffMultiplier: Double = 3.0,
)
