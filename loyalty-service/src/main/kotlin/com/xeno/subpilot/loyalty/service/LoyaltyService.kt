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
package com.xeno.subpilot.loyalty.service

import com.xeno.subpilot.loyalty.client.SubscriptionGrpcClient
import com.xeno.subpilot.loyalty.dto.SpendDenialReason
import com.xeno.subpilot.loyalty.dto.SpendResult
import com.xeno.subpilot.loyalty.dto.kafka.PaymentSucceededEvent
import com.xeno.subpilot.loyalty.properties.LoyaltyProperties
import com.xeno.subpilot.loyalty.repository.LoyaltyTransactionJpaRepository
import com.xeno.subpilot.loyalty.repository.UserLoyaltyBalanceJpaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class LoyaltyService(
    private val loyaltyProperties: LoyaltyProperties,
    private val loyaltyTransactionJpaRepository: LoyaltyTransactionJpaRepository,
    private val userLoyaltyBalanceJpaRepository: UserLoyaltyBalanceJpaRepository,
    private val subscriptionGrpcClient: SubscriptionGrpcClient,
    private val clock: Clock,
) {

    @Transactional
    fun earn(event: PaymentSucceededEvent) {
        val points =
            event.amount
                .multiply(loyaltyProperties.cashbackRate)
                .setScale(0, RoundingMode.FLOOR)
                .toLong()

        if (points <= 0) {
            logger.atInfo {
                message = "loyalty_earn_zero_points_skipped"
                payload =
                    mapOf(
                        "payment_id" to event.paymentId,
                        "user_id" to event.userId,
                        "amount" to event.amount,
                    )
            }
            return
        }

        val inserted =
            loyaltyTransactionJpaRepository.insertEarnedIfAbsent(
                userId = event.userId,
                amount = points,
                paymentId = event.paymentId,
                createdAt = LocalDateTime.now(clock),
            )

        if (inserted == 0) {
            logger.atInfo {
                message = "loyalty_earn_duplicate_payment_skipped"
                payload = mapOf("payment_id" to event.paymentId, "user_id" to event.userId)
            }
            return
        }

        userLoyaltyBalanceJpaRepository.upsertAdd(event.userId, points)

        logger.atInfo {
            message = "loyalty_earn_credited"
            payload =
                mapOf(
                    "payment_id" to event.paymentId,
                    "user_id" to event.userId,
                    "points" to points,
                )
        }

        if (event.bonusPointsUsed > 0) {
            deductBonusPointsUsed(event)
        }
    }

    private fun deductBonusPointsUsed(event: PaymentSucceededEvent) {
        val inserted =
            loyaltyTransactionJpaRepository.insertSpentIfAbsent(
                userId = event.userId,
                amount = event.bonusPointsUsed,
                paymentId = event.paymentId,
                createdAt = LocalDateTime.now(clock),
            )
        if (inserted == 0) return

        val deducted =
            userLoyaltyBalanceJpaRepository.deductIfSufficient(event.userId, event.bonusPointsUsed)
        if (deducted == 0) {
            logger.atWarn {
                message = "loyalty_bonus_deduct_insufficient_balance"
                payload =
                    mapOf(
                        "payment_id" to event.paymentId,
                        "user_id" to event.userId,
                        "bonus_points_used" to event.bonusPointsUsed,
                    )
            }
        }
    }

    @Transactional(readOnly = true)
    fun getBalance(userId: Long): Long =
        userLoyaltyBalanceJpaRepository.findPointsByUserId(userId) ?: 0L

    @Transactional
    fun spend(
        userId: Long,
        planId: String,
        idempotencyKey: UUID,
    ): SpendResult {
        val plan =
            subscriptionGrpcClient.getPlanInfo(planId)
                ?: return SpendResult.Denied(SpendDenialReason.UNKNOWN_PLAN)

        val price =
            BigDecimal(plan.price)
                .setScale(0, RoundingMode.CEILING)
                .longValueExact()

        val deducted = userLoyaltyBalanceJpaRepository.deductIfSufficient(userId, price)
        if (deducted == 0) {
            logger.atInfo {
                message = "loyalty_spend_insufficient_points"
                payload = mapOf("user_id" to userId, "plan_id" to planId, "price" to price)
            }
            return SpendResult.Denied(SpendDenialReason.INSUFFICIENT_POINTS)
        }

        val inserted =
            loyaltyTransactionJpaRepository.insertSpentIfAbsent(
                userId = userId,
                amount = price,
                paymentId = idempotencyKey,
                createdAt = LocalDateTime.now(clock),
            )
        if (inserted == 0) {
            userLoyaltyBalanceJpaRepository.upsertAdd(userId, price)
            return SpendResult.Success
        }

        subscriptionGrpcClient.activateSubscription(userId, planId, idempotencyKey)

        logger.atInfo {
            message = "loyalty_spend_activated"
            payload = mapOf("user_id" to userId, "plan_id" to planId, "price" to price)
        }

        return SpendResult.Success
    }
}
