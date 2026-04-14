package com.xeno.subpilot.subscription.repository

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
}
