package com.xeno.subpilot.subscription.repository

import com.xeno.subpilot.subscription.entity.SubscriptionUser
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class JpaSubscriptionUserRepository(
    private val repository: SubscriptionUserJpaRepository,
) : SubscriptionUserRepository {

    override fun findById(userId: Long): SubscriptionUser? = repository.findByIdOrNull(userId)

    override fun insertIfAbsent(userId: Long): Boolean = repository.insertIfAbsent(userId) > 0
}
