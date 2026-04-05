package com.xeno.subpilot.tgbot.properties

import org.springframework.boot.context.properties.ConfigurationProperties

import java.time.Duration

@ConfigurationProperties(prefix = "navigation")
data class NavigationProperties(
    val stackTtl: Duration,
)
