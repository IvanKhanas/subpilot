package com.xeno.subpilot.chat.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "grpc.retry")
data class GrpcRetryProperties(
    val maxAttempts: Int,
    val initialBackoffMs: Long,
    val backoffMultiplier: Double,
)
