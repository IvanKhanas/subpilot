package com.xeno.subpilot.subscription.repository

import com.xeno.subpilot.subscription.entity.UserRequestBalance

interface UserRequestBalanceRepository {
    fun deductIfSufficient(
        userId: Long,
        provider: String,
        cost: Int,
    ): Boolean

    fun addRequests(
        userId: Long,
        provider: String,
        amount: Int,
    )

    fun getRequestsRemaining(
        userId: Long,
        provider: String,
    ): Int

    fun findAllByUserId(userId: Long): List<UserRequestBalance>
}
