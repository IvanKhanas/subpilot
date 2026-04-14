package com.xeno.subpilot.subscription.service

import com.xeno.subpilot.subscription.properties.SubscriptionProperties
import com.xeno.subpilot.subscription.repository.SubscriptionUserRepository
import com.xeno.subpilot.subscription.repository.UserFreeQuotaRepository
import com.xeno.subpilot.subscription.repository.UserModelPreferenceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

@Service
class UserService(
    private val subscriptionUserRepository: SubscriptionUserRepository,
    private val freeQuotaRepository: UserFreeQuotaRepository,
    private val modelPreferenceRepository: UserModelPreferenceRepository,
    private val properties: SubscriptionProperties,
) {

    @Transactional
    fun registerUser(userId: Long): Boolean {
        val isNew = subscriptionUserRepository.insertIfAbsent(userId)
        if (!isNew) return false

        freeQuotaRepository.createAll(
            userId,
            properties.modelProviders.values.toSet(),
            properties.freeQuota,
            LocalDateTime.now().plus(properties.freeQuotaResetPeriod),
        )
        modelPreferenceRepository.upsert(userId, properties.defaultModel)
        return true
    }
}
