package com.xeno.subpilot.subscription.repository

import com.xeno.subpilot.subscription.entity.UserRequestBalance
import com.xeno.subpilot.subscription.entity.UserRequestBalanceId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface UserRequestBalanceJpaRepository :
    JpaRepository<UserRequestBalance, UserRequestBalanceId> {

    @Modifying(clearAutomatically = true)
    @Query(
        """
        UPDATE UserRequestBalance b
        SET b.requestsRemaining = b.requestsRemaining - :cost
        WHERE b.userId = :userId AND b.provider = :provider AND b.requestsRemaining >= :cost
        """,
    )
    fun deductIfSufficient(
        userId: Long,
        provider: String,
        cost: Int,
    ): Int

    @Modifying(clearAutomatically = true)
    @Query(
        """
        UPDATE UserRequestBalance b
        SET b.requestsRemaining = b.requestsRemaining + :amount
        WHERE b.userId = :userId AND b.provider = :provider
        """,
    )
    fun addRequests(
        userId: Long,
        provider: String,
        amount: Int,
    )

    @Query("SELECT b FROM UserRequestBalance b WHERE b.userId = :userId AND b.provider = :provider")
    fun findByUserIdAndProvider(
        userId: Long,
        provider: String,
    ): UserRequestBalance?
}
