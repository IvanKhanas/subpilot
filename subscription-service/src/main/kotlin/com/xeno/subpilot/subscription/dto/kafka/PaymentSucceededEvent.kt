package com.xeno.subpilot.subscription.dto.kafka

import java.math.BigDecimal
import java.util.UUID

data class PaymentSucceededEvent(
    val paymentId: UUID,
    val userId: Long,
    val planId: String,
    val amount: BigDecimal,
)
