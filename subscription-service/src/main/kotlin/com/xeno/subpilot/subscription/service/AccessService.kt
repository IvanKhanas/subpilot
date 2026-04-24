/*
 * Copyright 2024 Ivan Khanas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        if (freeToUse > 0) {
            freeQuotaRepository.deductRequests(userId, provider, freeToUse)
        }
        val freeQuotaJustExhausted = freeToUse > 0 && (quota.requestsRemaining - freeToUse) == 0
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
