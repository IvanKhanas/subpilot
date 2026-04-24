package com.xeno.subpilot.subscription.repository

import com.xeno.subpilot.subscription.entity.UserFreeQuota

import java.time.LocalDateTime

interface UserFreeQuotaRepository {
    fun findByUserIdAndProviderForUpdate(
        userId: Long,
        provider: String,
    ): UserFreeQuota?

    fun addRequests(
        userId: Long,
        provider: String,
        amount: Int,
    )

    fun deductRequests(
        userId: Long,
        provider: String,
        amount: Int,
    )

    fun createAll(
        userId: Long,
        providers: Set<String>,
        initialAmount: Int,
        nextResetAt: LocalDateTime,
    )

    fun findAllByUserId(userId: Long): List<UserFreeQuota>
}
