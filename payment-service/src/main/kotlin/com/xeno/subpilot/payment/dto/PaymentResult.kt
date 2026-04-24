package com.xeno.subpilot.payment.dto

data class PaymentResult(
    val paymentId: String,
    val confirmationUrl: String,
)
