package com.xeno.subpilot.subscription.repository

import org.springframework.stereotype.Repository

@Repository
class JpaUserRequestBalanceRepository(
    private val repository: UserRequestBalanceJpaRepository,
) : UserRequestBalanceRepository {

    override fun deductIfSufficient(
        userId: Long,
        provider: String,
        cost: Int,
    ): Boolean = repository.deductIfSufficient(userId, provider, cost) > 0

    override fun addRequests(
        userId: Long,
        provider: String,
        amount: Int,
    ) {
        repository.addRequests(userId, provider, amount)
    }

    override fun getRequestsRemaining(
        userId: Long,
        provider: String,
    ): Int = repository.findByUserIdAndProvider(userId, provider)?.requestsRemaining ?: 0
}
