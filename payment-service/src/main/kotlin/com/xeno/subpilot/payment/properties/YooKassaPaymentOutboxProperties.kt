package com.xeno.subpilot.payment.properties

import org.springframework.boot.context.properties.ConfigurationProperties

import java.time.Duration

@ConfigurationProperties(prefix = "yookassa-outbox")
data class YooKassaPaymentOutboxProperties(
    val schedulerInterval: Duration,
    val batchSize: Int,
)
