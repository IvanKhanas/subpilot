package com.xeno.subpilot.tgbot.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "telegram.bot")
data class TelegramBotProperties(
    val token: String,
    val pollingTimeout: Int,
    val baseUrl: String = "https://api.telegram.org",
)
