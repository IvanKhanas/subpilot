package com.xeno.subpilot.payment.dto.kafka

import java.util.UUID

data class YooKassaWebhookPayment(
    val id: UUID,
    val status: String,
)
