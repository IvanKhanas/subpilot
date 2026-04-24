package com.xeno.subpilot.payment.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "yookassa")
data class YooKassaProperties(
    val shopId: String,
    val secretKey: String,
    val returnUrl: String,
    val apiBaseUrl: String = "https://api.yookassa.ru/v3",
)
