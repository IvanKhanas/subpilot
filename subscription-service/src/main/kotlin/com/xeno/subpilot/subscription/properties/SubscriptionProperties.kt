package com.xeno.subpilot.subscription.properties

import org.springframework.boot.context.properties.ConfigurationProperties

import java.time.Duration

@ConfigurationProperties(prefix = "subscription")
data class SubscriptionProperties(
    val freeQuota: Int,
    val freeQuotaResetPeriod: Duration,
    val defaultModel: String,
    val modelProviders: Map<String, String>,
    val modelCosts: Map<String, Int>,
)
