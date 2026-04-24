package com.xeno.subpilot.subscription.unittests.service

import com.xeno.subpilot.subscription.dto.kafka.PaymentSucceededEvent
import com.xeno.subpilot.subscription.properties.PlanProperties
import com.xeno.subpilot.subscription.properties.ProviderAllocation
import com.xeno.subpilot.subscription.properties.SubscriptionProperties
import com.xeno.subpilot.subscription.repository.UserSubscriptionActivationRepository
import com.xeno.subpilot.subscription.service.SubscriptionActivationService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class SubscriptionActivationServiceTest {

    @MockK
    lateinit var activationRepository: UserSubscriptionActivationRepository

    private val fixedClock: Clock =
        Clock.fixed(
            Instant.parse("2026-04-24T12:00:00Z"),
            ZoneOffset.UTC,
        )

    private val openAiAllocation = ProviderAllocation(provider = "openai", requests = 100)
    private val anthropicAllocation = ProviderAllocation(provider = "anthropic", requests = 50)

    private val properties =
        SubscriptionProperties(
            freeQuota = 10,
            freeQuotaResetPeriod = Duration.ofDays(7),
            defaultModel = "gpt-4o-mini",
            modelProviders = mapOf("gpt-4o-mini" to "openai"),
            modelCosts = mapOf("gpt-4o-mini" to 1),
            plans =
                mapOf(
                    "combo-basic" to
                        PlanProperties(
                            provider = "openai",
                            displayName = "Combo Basic",
                            price = BigDecimal("299.00"),
                            currency = "RUB",
                            allocations = listOf(openAiAllocation, anthropicAllocation),
                        ),
                ),
        )

    private lateinit var service: SubscriptionActivationService

    @BeforeEach
    fun setUp() {
        service = SubscriptionActivationService(properties, activationRepository, fixedClock)
        justRun { activationRepository.batchUpsertRequestBalance(any(), any()) }
    }

    @Test
    fun `activate returns false when plan is unknown`() {
        val result =
            service.activate(
                PaymentSucceededEvent(
                    paymentId = UUID.randomUUID(),
                    userId = 42L,
                    planId = "unknown-plan",
                    amount = BigDecimal("100.00"),
                ),
            )

        assertFalse(result)
        verify(exactly = 0) {
            activationRepository.batchInsertUserSubscriptionIfAbsent(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `activateDirect returns false when idempotent insert returns empty providers`() {
        val idempotencyKey = UUID.randomUUID()
        every {
            activationRepository.batchInsertUserSubscriptionIfAbsent(
                paymentId = idempotencyKey,
                userId = 42L,
                planId = "combo-basic",
                allocations = listOf(openAiAllocation, anthropicAllocation),
                activatedAt = fixedClock.instant(),
            )
        } returns emptyList()

        val result = service.activateDirect(42L, "combo-basic", idempotencyKey)

        assertFalse(result)
        verify(exactly = 0) { activationRepository.batchUpsertRequestBalance(any(), any()) }
    }

    @Test
    fun `activateDirect upserts balances only for providers that were inserted`() {
        val idempotencyKey = UUID.randomUUID()
        every {
            activationRepository.batchInsertUserSubscriptionIfAbsent(
                paymentId = idempotencyKey,
                userId = 42L,
                planId = "combo-basic",
                allocations = listOf(openAiAllocation, anthropicAllocation),
                activatedAt = fixedClock.instant(),
            )
        } returns listOf("openai")

        val result = service.activateDirect(42L, "combo-basic", idempotencyKey)

        assertTrue(result)
        verify {
            activationRepository.batchUpsertRequestBalance(
                userId = 42L,
                allocations = listOf(openAiAllocation),
            )
        }
    }

    @Test
    fun `activate delegates event fields to activation pipeline`() {
        val paymentId = UUID.randomUUID()
        every {
            activationRepository.batchInsertUserSubscriptionIfAbsent(
                paymentId = paymentId,
                userId = 7L,
                planId = "combo-basic",
                allocations = listOf(openAiAllocation, anthropicAllocation),
                activatedAt = fixedClock.instant(),
            )
        } returns listOf("openai", "anthropic")

        val result =
            service.activate(
                PaymentSucceededEvent(
                    paymentId = paymentId,
                    userId = 7L,
                    planId = "combo-basic",
                    amount = BigDecimal("299.00"),
                ),
            )

        assertTrue(result)
        verify {
            activationRepository.batchUpsertRequestBalance(
                userId = 7L,
                allocations = listOf(openAiAllocation, anthropicAllocation),
            )
        }
    }
}
