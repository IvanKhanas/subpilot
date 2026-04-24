package com.xeno.subpilot.subscription.dto

data class BalanceInfo(
    val freeBalances: List<FreeProviderBalance>,
    val paidBalances: List<PaidProviderBalance>,
)
