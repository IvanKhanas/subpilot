package com.xeno.subpilot.subscription.dto

import java.time.LocalDateTime

data class FreeProviderBalance(
    val provider: String,
    val requestsRemaining: Int,
    val nextResetAt: LocalDateTime,
)

data class PaidProviderBalance(
    val provider: String,
    val requestsRemaining: Int,
)
