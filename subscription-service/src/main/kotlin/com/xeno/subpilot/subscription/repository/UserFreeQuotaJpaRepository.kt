package com.xeno.subpilot.subscription.repository

import com.xeno.subpilot.subscription.entity.UserFreeQuota
import com.xeno.subpilot.subscription.entity.UserFreeQuotaId
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface UserFreeQuotaJpaRepository : JpaRepository<UserFreeQuota, UserFreeQuotaId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM UserFreeQuota q WHERE q.userId = :userId AND q.provider = :provider")
    fun findByUserIdAndProviderForUpdate(
        userId: Long,
        provider: String,
    ): UserFreeQuota?

    @Modifying(clearAutomatically = true)
    @Query(
        """
        UPDATE UserFreeQuota q
        SET q.requestsRemaining = q.requestsRemaining + :amount
        WHERE q.userId = :userId AND q.provider = :provider
        """,
    )
    fun addRequests(
        userId: Long,
        provider: String,
        amount: Int,
    )

    @Modifying(clearAutomatically = true)
    @Query(
        """
        UPDATE UserFreeQuota q
        SET q.requestsRemaining = q.requestsRemaining - :amount
        WHERE q.userId = :userId AND q.provider = :provider
        """,
    )
    fun deductRequests(
        userId: Long,
        provider: String,
        amount: Int,
    )

    fun findAllByUserId(userId: Long): List<UserFreeQuota>
}
