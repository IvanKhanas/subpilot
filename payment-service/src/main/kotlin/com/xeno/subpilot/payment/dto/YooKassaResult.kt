package com.xeno.subpilot.payment.dto

import java.util.UUID

data class YooKassaResult(
    val yookassaPaymentId: UUID,
    val confirmationUrl: String,
)
