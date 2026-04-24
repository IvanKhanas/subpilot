package com.xeno.subpilot.subscription.dto.kafka

data class SubscriptionActivatedEvent(
    val userId: Long,
    val planDisplayName: String,
    val allocations: List<ProviderAllocation>,
) {
    data class ProviderAllocation(
        val provider: String,
        val requests: Int,
    )
}
