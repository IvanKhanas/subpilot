package com.xeno.subpilot.subscription.repository

import com.xeno.subpilot.subscription.entity.SubscriptionUser

interface SubscriptionUserRepository {
    fun findById(userId: Long): SubscriptionUser?

    fun insertIfAbsent(userId: Long): Boolean
}
