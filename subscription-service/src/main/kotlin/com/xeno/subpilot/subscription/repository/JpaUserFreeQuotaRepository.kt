package com.xeno.subpilot.subscription.repository

import com.xeno.subpilot.subscription.entity.UserFreeQuota
import org.springframework.stereotype.Repository

import java.time.LocalDateTime

@Repository
class JpaUserFreeQuotaRepository(
    private val repository: UserFreeQuotaJpaRepository,
) : UserFreeQuotaRepository {

    override fun findByUserIdAndProviderForUpdate(
        userId: Long,
        provider: String,
    ): UserFreeQuota? = repository.findByUserIdAndProviderForUpdate(userId, provider)

    override fun addRequests(
        userId: Long,
        provider: String,
        amount: Int,
    ) = repository.addRequests(userId, provider, amount)

    override fun deductRequests(
        userId: Long,
        provider: String,
        amount: Int,
    ) = repository.deductRequests(userId, provider, amount)

    override fun createAll(
        userId: Long,
        providers: Set<String>,
        initialAmount: Int,
        nextResetAt: LocalDateTime,
    ) {
        repository.saveAll(
            providers.map { provider ->
                UserFreeQuota(userId, provider, initialAmount, nextResetAt)
            },
        )
    }

    override fun findAllByUserId(userId: Long): List<UserFreeQuota> =
        repository.findAllByUserId(userId)
}
