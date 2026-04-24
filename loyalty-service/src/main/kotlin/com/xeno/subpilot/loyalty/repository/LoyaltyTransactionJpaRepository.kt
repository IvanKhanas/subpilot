package com.xeno.subpilot.loyalty.repository

import com.xeno.subpilot.loyalty.entity.LoyaltyTransaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

import java.time.LocalDateTime
import java.util.UUID

@Repository
interface LoyaltyTransactionJpaRepository : JpaRepository<LoyaltyTransaction, Long> {

    @Modifying(clearAutomatically = true)
    @Query(
        value = """
            INSERT INTO loyalty_transaction (user_id, amount, type, payment_id, created_at)
            VALUES (:userId, :amount, 'EARNED', :paymentId, :createdAt)
            ON CONFLICT (payment_id, type) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertEarnedIfAbsent(
        userId: Long,
        amount: Long,
        paymentId: UUID,
        createdAt: LocalDateTime,
    ): Int

    @Modifying(clearAutomatically = true)
    @Query(
        value = """
            INSERT INTO loyalty_transaction (user_id, amount, type, payment_id, created_at)
            VALUES (:userId, :amount, 'SPENT', :paymentId, :createdAt)
            ON CONFLICT (payment_id, type) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertSpentIfAbsent(
        userId: Long,
        amount: Long,
        paymentId: UUID,
        createdAt: LocalDateTime,
    ): Int
}
