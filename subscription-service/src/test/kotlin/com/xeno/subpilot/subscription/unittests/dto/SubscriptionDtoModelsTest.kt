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
