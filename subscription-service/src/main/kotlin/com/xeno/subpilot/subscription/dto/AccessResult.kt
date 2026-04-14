package com.xeno.subpilot.subscription.dto

import java.time.LocalDateTime

data class AccessResult(
    val allowed: Boolean,
    val denialReason: DenialReason = DenialReason.UNSPECIFIED,
    val availableRequests: Int = 0,
    val modelCost: Int = 0,
    val resetAt: LocalDateTime? = null,
    val freeConsumed: Int = 0,
    val paidConsumed: Int = 0,
)
