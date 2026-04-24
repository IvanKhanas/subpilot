package com.xeno.subpilot.loyalty.repository

import com.xeno.subpilot.loyalty.entity.UserLoyaltyBalance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserLoyaltyBalanceJpaRepository : JpaRepository<UserLoyaltyBalance, Long> {

    @Modifying(clearAutomatically = true)
    @Query(
        value = """
            INSERT INTO user_loyalty_balance (user_id, points)
            VALUES (:userId, :amount)
            ON CONFLICT (user_id)
            DO UPDATE SET points = user_loyalty_balance.points + EXCLUDED.points
        """,
        nativeQuery = true,
    )
    fun upsertAdd(
        userId: Long,
        amount: Long,
    )

    @Modifying(clearAutomatically = true)
    @Query(
        """
        UPDATE UserLoyaltyBalance b
        SET b.points = b.points - :amount
        WHERE b.userId = :userId AND b.points >= :amount
        """,
    )
    fun deductIfSufficient(
        userId: Long,
        amount: Long,
    ): Int

    @Query(
        """
        SELECT b.points
        FROM UserLoyaltyBalance b
        WHERE b.userId = :userId
        """,
    )
    fun findPointsByUserId(userId: Long): Long?
}
