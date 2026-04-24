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
package com.xeno.subpilot.tgbot.unittests.dto

import com.xeno.subpilot.tgbot.dto.BalanceInfo
import com.xeno.subpilot.tgbot.dto.FreeProviderBalance
import com.xeno.subpilot.tgbot.dto.PaidProviderBalance
import com.xeno.subpilot.tgbot.dto.PlanAllocation
import com.xeno.subpilot.tgbot.dto.PlanInfo
import com.xeno.subpilot.tgbot.dto.SpendDenialReason
import com.xeno.subpilot.tgbot.dto.SpendResult
import com.xeno.subpilot.tgbot.dto.kafka.SubscriptionActivatedEvent
import org.junit.jupiter.api.Test

import java.time.Instant

import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PremiumModelsTest {

    @Test
    fun `balance and plan dto objects keep provided values`() {
        val free = FreeProviderBalance("openai", 7, Instant.parse("2026-04-24T12:00:00Z"))
        val paid = PaidProviderBalance("openai", 120)
        val balance = BalanceInfo(freeBalances = listOf(free), paidBalances = listOf(paid))
        val plan =
            PlanInfo(
                planId = "openai-basic",
                provider = "openai",
                displayName = "Basic",
                price = "199.00",
                currency = "RUB",
                allocations = listOf(PlanAllocation("openai", 100)),
            )

        assertEquals("openai", balance.freeBalances.single().provider)
        assertEquals(7, balance.freeBalances.single().requestsRemaining)
        assertEquals(120, balance.paidBalances.single().requestsRemaining)
        assertEquals("openai-basic", plan.planId)
        assertEquals("Basic", plan.displayName)
        assertEquals(100, plan.allocations.single().requests)
    }

    @Test
    fun `spend result denied keeps denial reason enum`() {
        val denied = SpendResult.Denied(SpendDenialReason.UNKNOWN_PLAN)

        assertIs<SpendResult.Denied>(denied)
        assertEquals(SpendDenialReason.UNKNOWN_PLAN, denied.reason)
        assertTrue(SpendDenialReason.entries.contains(SpendDenialReason.UNSPECIFIED))
    }

    @Test
    fun `subscription activated kafka dto keeps allocations`() {
        val event =
            SubscriptionActivatedEvent(
                userId = 42L,
                planDisplayName = "Basic",
                allocations =
                    listOf(
                        SubscriptionActivatedEvent.ProviderAllocation("openai", 100),
                    ),
            )

        assertEquals(42L, event.userId)
        assertEquals("Basic", event.planDisplayName)
        assertEquals("openai", event.allocations.single().provider)
        assertEquals(100, event.allocations.single().requests)
    }
}
