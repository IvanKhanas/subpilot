package com.xeno.subpilot.tgbot.dto

data class PlanAllocation(
    val provider: String,
    val requests: Int,
)

data class PlanInfo(
    val planId: String,
    val provider: String,
    val displayName: String,
    val price: String,
    val currency: String,
    val allocations: List<PlanAllocation>,
)
