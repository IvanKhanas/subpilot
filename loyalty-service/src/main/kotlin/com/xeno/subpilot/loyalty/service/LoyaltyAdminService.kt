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

import com.xeno.subpilot.loyalty.repository.LoyaltyTransactionJpaRepository
import com.xeno.subpilot.loyalty.repository.UserLoyaltyBalanceJpaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class LoyaltyAdminService(
    private val loyaltyTransactionJpaRepository: LoyaltyTransactionJpaRepository,
    private val userLoyaltyBalanceJpaRepository: UserLoyaltyBalanceJpaRepository,
    private val clock: Clock,
) {

    @Transactional
    fun adjustPoints(userId: Long, delta: Long, reason: String, idempotencyKey: UUID) {
        require(delta != 0L) { "delta cannot be zero" }

        val inserted = loyaltyTransactionJpaRepository.insertAdjustedIfAbsent(
            userId = userId,
            amount = delta,
            idempotencyKey = idempotencyKey,
            reason = reason,
            createdAt = LocalDateTime.now(clock),
        )

        if (inserted == 0) {
            logger.atInfo {
                message = "loyalty_adjust_duplicate_skipped"
                payload = mapOf("user_id" to userId, "idempotency_key" to idempotencyKey)
            }
            return
        }

        if (delta > 0) {
            userLoyaltyBalanceJpaRepository.upsertAdd(userId, delta)
        } else {
            userLoyaltyBalanceJpaRepository.subtractCappedAtZero(userId, -delta)
        }

        logger.atInfo {
            message = "loyalty_admin_adjusted"
            payload = mapOf("user_id" to userId, "delta" to delta, "reason" to reason)
        }
    }
}