package com.xeno.subpilot.subscription.repository

import com.xeno.subpilot.subscription.properties.ProviderAllocation

import java.time.Instant
import java.util.UUID

interface UserSubscriptionActivationRepository {

    fun batchInsertUserSubscriptionIfAbsent(
        paymentId: UUID,
        userId: Long,
        planId: String,
        allocations: List<ProviderAllocation>,
        activatedAt: Instant,
    ): List<String>

    fun batchUpsertRequestBalance(
        userId: Long,
        allocations: List<ProviderAllocation>,
    )
}
