package com.xeno.subpilot.tgbot.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "support")
data class SupportProperties(
    val operatorTag: String,
)
