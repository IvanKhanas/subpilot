package com.xeno.subpilot.subscription.service

import com.xeno.subpilot.subscription.dto.AccessResult
import com.xeno.subpilot.subscription.dto.DenialReason
import com.xeno.subpilot.subscription.entity.UserFreeQuota
import com.xeno.subpilot.subscription.properties.SubscriptionProperties
import com.xeno.subpilot.subscription.repository.SubscriptionUserRepository
import com.xeno.subpilot.subscription.repository.UserFreeQuotaRepository
import com.xeno.subpilot.subscription.repository.UserRequestBalanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.LocalDateTime

@Service
class AccessService(
    private val freeQuotaRepository: UserFreeQuotaRepository,
    private val balanceRepository: UserRequestBalanceRepository,
    private val subscriptionUserRepository: SubscriptionUserRepository,
    private val properties: SubscriptionProperties,
) {

    @Transactional
    fun checkAndConsume(
        userId: Long,
        modelId: String,
    ): AccessResult {
        val (provider, cost) =
            resolveProviderAndCost(modelId)
                ?: return AccessResult(allowed = false, denialReason = DenialReason.NO_SUBSCRIPTION)

        val user = subscriptionUserRepository.findById(userId)
        if (user?.blocked == true) {
            return AccessResult(allowed = false, denialReason = DenialReason.BLOCKED)
        }

        val quota =
            freeQuotaRepository.findByUserIdAndProviderForUpdate(userId, provider)
                ?: return AccessResult(allowed = false, denialReason = DenialReason.QUOTA_EXHAUSTED)

        resetQuotaIfExpired(quota)

        return consumeBalance(userId, provider, quota, cost)
    }

    private fun resetQuotaIfExpired(quota: UserFreeQuota) {
        val now = LocalDateTime.now()
        if (quota.nextResetAt.isBefore(now)) {
            quota.requestsRemaining = properties.freeQuota
            quota.nextResetAt = now.plus(properties.freeQuotaResetPeriod)
        }
    }

    private fun consumeBalance(
        userId: Long,
        provider: String,
        quota: UserFreeQuota,
        cost: Int,
    ): AccessResult {
        val freeToUse = minOf(quota.requestsRemaining, cost)
        val paidNeeded = cost - freeToUse
        if (paidNeeded > 0 && !balanceRepository.deductIfSufficient(userId, provider, paidNeeded)) {
            val availableRequests =
                quota.requestsRemaining + balanceRepository.getRequestsRemaining(userId, provider)
            return AccessResult(
                allowed = false,
                denialReason = DenialReason.NO_SUBSCRIPTION,
                availableRequests = availableRequests,
                modelCost = cost,
            )
        }
        quota.requestsRemaining -= freeToUse
        val freeQuotaJustExhausted = freeToUse > 0 && quota.requestsRemaining == 0
        return if (freeQuotaJustExhausted) {
            AccessResult(
                allowed = true,
                resetAt = quota.nextResetAt,
                freeConsumed = freeToUse,
                paidConsumed = paidNeeded,
            )
        } else {
            AccessResult(allowed = true, freeConsumed = freeToUse, paidConsumed = paidNeeded)
        }
    }

    private fun resolveProviderAndCost(modelId: String): Pair<String, Int>? {
        val provider = properties.modelProviders[modelId] ?: return null
        val cost = properties.modelCosts[modelId] ?: return null
        return provider to cost
    }

    @Transactional
    fun refund(
        userId: Long,
        modelId: String,
        freeConsumed: Int,
        paidConsumed: Int,
    ) {
        val provider = properties.modelProviders[modelId] ?: return

        if (freeConsumed > 0) {
            freeQuotaRepository.addRequests(userId, provider, freeConsumed)
        }
        if (paidConsumed > 0) {
            balanceRepository.addRequests(userId, provider, paidConsumed)
        }
    }
}
