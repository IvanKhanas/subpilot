package com.xeno.subpilot.subscription.service

import com.xeno.subpilot.subscription.properties.SubscriptionProperties
import com.xeno.subpilot.subscription.repository.SubscriptionUserRepository
import com.xeno.subpilot.subscription.repository.UserRequestBalanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AccessService(
    private val balanceRepository: UserRequestBalanceRepository,
    private val subscriptionUserRepository: SubscriptionUserRepository,
    private val properties: SubscriptionProperties,
) {

    @Transactional
    fun checkAndConsume(
        userId: Long,
        modelId: String,
    ): AccessResult {
        val provider = properties.modelProviders[modelId]
            ?: return AccessResult(allowed = false, denialReason = DenialReason.NO_SUBSCRIPTION)

        val modelCost = properties.modelCosts[modelId]
            ?: return AccessResult(allowed = false, denialReason = DenialReason.NO_SUBSCRIPTION)

        val subscriptionUser = subscriptionUserRepository.findById(userId)
        if (subscriptionUser?.blocked == true) {
            return AccessResult(allowed = false, denialReason = DenialReason.BLOCKED)
        }

        val deducted = balanceRepository.deductIfSufficient(userId, provider, modelCost)
        return if (deducted) {
            AccessResult(allowed = true)
        } else {
            AccessResult(allowed = false, denialReason = DenialReason.QUOTA_EXHAUSTED)
        }
    }

    @Transactional
    fun refund(
        userId: Long,
        modelId: String,
    ) {
        val provider = properties.modelProviders[modelId] ?: return
        val modelCost = properties.modelCosts[modelId] ?: return
        balanceRepository.addRequests(userId, provider, modelCost)
    }
}
