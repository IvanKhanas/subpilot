package com.xeno.subpilot.tgbot.dto

import java.time.Instant

data class FreeProviderBalance(
    val provider: String,
    val requestsRemaining: Int,
    val nextResetAt: Instant,
)

data class PaidProviderBalance(
    val provider: String,
    val requestsRemaining: Int,
)

data class BalanceInfo(
    val freeBalances: List<FreeProviderBalance>,
    val paidBalances: List<PaidProviderBalance>,
)
