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

import com.xeno.subpilot.subscription.dto.kafka.PaymentSucceededEvent
import com.xeno.subpilot.subscription.repository.PlanRepository
import com.xeno.subpilot.subscription.repository.UserSubscriptionActivationRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.Clock
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class SubscriptionActivationService(
    private val planRepository: PlanRepository,
    private val activationRepository: UserSubscriptionActivationRepository,
    private val clock: Clock,
) {

    @Transactional
    fun activate(event: PaymentSucceededEvent): Boolean =
        activateInternal(event.userId, event.planId, event.paymentId)

    @Transactional
    fun activateDirect(
        userId: Long,
        planId: String,
        idempotencyKey: UUID,
    ): Boolean = activateInternal(userId, planId, idempotencyKey)

    private fun activateInternal(
        userId: Long,
        planId: String,
        paymentId: UUID,
    ): Boolean {
        val plan = planRepository.findById(planId)
        if (plan == null) {
            logger.atWarn {
                message = "subscription_activation_unknown_plan"
                payload = mapOf("plan_id" to planId, "payment_id" to paymentId)
            }
            return false
        }

        val activatedAt: Instant = clock.instant()
        val insertedProviders =
            activationRepository
                .batchInsertUserSubscriptionIfAbsent(
                    paymentId = paymentId,
                    userId = userId,
                    planId = planId,
                    allocations = plan.allocations,
                    activatedAt = activatedAt,
                ).toSet()
        if (insertedProviders.isEmpty()) return false

        activationRepository.batchUpsertRequestBalance(
            userId = userId,
            allocations = plan.allocations.filter { it.provider in insertedProviders },
        )
        return true
    }
}
