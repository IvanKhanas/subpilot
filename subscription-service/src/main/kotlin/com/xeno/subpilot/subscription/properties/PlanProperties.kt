package com.xeno.subpilot.subscription.properties

data class ProviderAllocation(
    val provider: String,
    val requests: Int,
)

data class PlanProperties(
    val allocations: List<ProviderAllocation>,
)
