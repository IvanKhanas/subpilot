package com.xeno.subpilot.subscription.properties

import java.math.BigDecimal

data class ProviderAllocation(
    val provider: String,
    val requests: Int,
)

data class PlanProperties(
    val provider: String,
    val displayName: String,
    val price: BigDecimal,
    val currency: String,
    val allocations: List<ProviderAllocation>,
)
