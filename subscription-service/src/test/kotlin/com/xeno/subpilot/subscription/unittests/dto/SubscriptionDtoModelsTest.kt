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
package com.xeno.subpilot.subscription.unittests.dto

import com.xeno.subpilot.subscription.dto.BalanceInfo
import com.xeno.subpilot.subscription.dto.FreeProviderBalance
import com.xeno.subpilot.subscription.dto.PaidProviderBalance
import com.xeno.subpilot.subscription.dto.kafka.PaymentSucceededEvent
import com.xeno.subpilot.subscription.dto.kafka.SubscriptionActivatedEvent
import org.junit.jupiter.api.Test

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

import kotlin.test.assertEquals

class SubscriptionDtoModelsTest {

    @Test
    fun `balance dto keeps free and paid entries`() {
        val resetAt = LocalDateTime.of(2026, 4, 24, 11, 0, 0)
        val free = FreeProviderBalance("openai", 7, resetAt)
        val paid = PaidProviderBalance("openai", 120)

        val balance = BalanceInfo(freeBalances = listOf(free), paidBalances = listOf(paid))

        assertEquals("openai", balance.freeBalances.single().provider)
        assertEquals(7, balance.freeBalances.single().requestsRemaining)
        assertEquals(resetAt, balance.freeBalances.single().nextResetAt)
        assertEquals(120, balance.paidBalances.single().requestsRemaining)
    }

    @Test
    fun `payment succeeded event keeps identifiers and amount`() {
        val paymentId = UUID.randomUUID()
        val event =
            PaymentSucceededEvent(
                paymentId = paymentId,
                userId = 42L,
                planId = "openai-basic",
                amount = BigDecimal("199.00"),
            )

        assertEquals(paymentId, event.paymentId)
        assertEquals(42L, event.userId)
        assertEquals("openai-basic", event.planId)
        assertEquals(BigDecimal("199.00"), event.amount)
    }

    @Test
    fun `subscription activated event keeps plan and allocations`() {
        val event =
            SubscriptionActivatedEvent(
                userId = 42L,
                planDisplayName = "Basic",
                allocations =
                    listOf(
                        SubscriptionActivatedEvent.ProviderAllocation(
                            provider = "openai",
                            requests = 100,
                        ),
                    ),
            )

        assertEquals(42L, event.userId)
        assertEquals("Basic", event.planDisplayName)
        assertEquals("openai", event.allocations.single().provider)
        assertEquals(100, event.allocations.single().requests)
    }
}
