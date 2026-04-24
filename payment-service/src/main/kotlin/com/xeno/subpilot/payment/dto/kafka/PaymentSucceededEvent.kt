package com.xeno.subpilot.payment.dto.kafka

import java.math.BigDecimal
import java.util.UUID

class PaymentSucceededEvent(
    val paymentId: UUID,
    val userId: Long,
    val planId: String,
    val amount: BigDecimal,
    val bonusPointsUsed: Long = 0,
)
