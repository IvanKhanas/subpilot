package com.xeno.subpilot.subscription.repository

import com.xeno.subpilot.subscription.entity.SubscriptionUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface SubscriptionUserJpaRepository : JpaRepository<SubscriptionUser, Long> {

    @Modifying
    @Query(
        value = """
            INSERT INTO subscription_user (user_id, registered_at, blocked, role)
            VALUES (:userId, NOW(), FALSE, 'USER')
            ON CONFLICT (user_id) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(userId: Long): Int
}
