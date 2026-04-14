package com.xeno.subpilot.subscription.unittests.service

import com.xeno.subpilot.subscription.dto.DenialReason
import com.xeno.subpilot.subscription.entity.SubscriptionUser
import com.xeno.subpilot.subscription.entity.UserFreeQuota
import com.xeno.subpilot.subscription.properties.SubscriptionProperties
import com.xeno.subpilot.subscription.repository.SubscriptionUserRepository
import com.xeno.subpilot.subscription.repository.UserFreeQuotaRepository
import com.xeno.subpilot.subscription.repository.UserRequestBalanceRepository
import com.xeno.subpilot.subscription.service.AccessService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

import java.time.Duration
import java.time.LocalDateTime

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class AccessServiceTest {

    @MockK
    lateinit var freeQuotaRepository: UserFreeQuotaRepository

    @MockK
    lateinit var balanceRepository: UserRequestBalanceRepository

    @MockK
    lateinit var subscriptionUserRepository: SubscriptionUserRepository

    private lateinit var service: AccessService

    private val properties =
        SubscriptionProperties(
            freeQuota = 10,
            freeQuotaResetPeriod = Duration.ofDays(7),
            defaultModel = "gpt-4o-mini",
            modelProviders = mapOf("gpt-4o" to "openai", "gpt-4o-mini" to "openai"),
            modelCosts = mapOf("gpt-4o" to 3, "gpt-4o-mini" to 1),
            plans = emptyMap(),
        )

    private val userId = 1L
    private val future = LocalDateTime.now().plusDays(7)

    @BeforeEach
    fun setUp() {
        service =
            AccessService(
                freeQuotaRepository,
                balanceRepository,
                subscriptionUserRepository,
                properties,
            )
        every { subscriptionUserRepository.findById(any()) } returns null
    }

    private fun quota(
        remaining: Int,
        nextResetAt: LocalDateTime = future,
    ) = UserFreeQuota(
        userId = userId,
        provider = "openai",
        requestsRemaining = remaining,
        nextResetAt = nextResetAt,
    )

    @Test
    fun `checkAndConsume denies when model not in config`() {
        val result = service.checkAndConsume(userId, "unknown-model")

        assertFalse(result.allowed)
        assertEquals(DenialReason.NO_SUBSCRIPTION, result.denialReason)
    }

    @Test
    fun `checkAndConsume denies when user is blocked`() {
        every { subscriptionUserRepository.findById(userId) } returns
            SubscriptionUser(userId = userId, blocked = true)

        val result = service.checkAndConsume(userId, "gpt-4o-mini")

        assertFalse(result.allowed)
        assertEquals(DenialReason.BLOCKED, result.denialReason)
    }

    @Test
    fun `checkAndConsume denies with QUOTA_EXHAUSTED when free quota row absent`() {
        every { freeQuotaRepository.findByUserIdAndProviderForUpdate(userId, "openai") } returns
            null

        val result = service.checkAndConsume(userId, "gpt-4o-mini")

        assertFalse(result.allowed)
        assertEquals(DenialReason.QUOTA_EXHAUSTED, result.denialReason)
    }

    @ParameterizedTest(
        name = "free={0}, cost=3 -> freeConsumed={1}, paidConsumed={2}, remaining={3}",
    )
    @CsvSource(
        "5, 3, 0, 2",
        "1, 1, 2, 0",
        "0, 0, 3, 0",
    )
    fun `checkAndConsume correctly splits deduction across free and paid quota`(
        freeRemaining: Int,
        expectedFreeConsumed: Int,
        expectedPaidConsumed: Int,
        expectedRemainingAfter: Int,
    ) {
        val quota = quota(remaining = freeRemaining)
        every { freeQuotaRepository.findByUserIdAndProviderForUpdate(userId, "openai") } returns
            quota
        if (expectedPaidConsumed > 0) {
            every {
                balanceRepository.deductIfSufficient(
                    userId,
                    "openai",
                    expectedPaidConsumed,
                )
            } returns
                true
        }

        val result = service.checkAndConsume(userId, "gpt-4o")

        assertTrue(result.allowed)
        assertEquals(expectedFreeConsumed, result.freeConsumed)
        assertEquals(expectedPaidConsumed, result.paidConsumed)
        assertEquals(expectedRemainingAfter, quota.requestsRemaining)
    }

    @Test
    fun `checkAndConsume denies with NO_SUBSCRIPTION when paid balance insufficient`() {
        val quota = quota(remaining = 0)
        every { freeQuotaRepository.findByUserIdAndProviderForUpdate(userId, "openai") } returns
            quota
        every { balanceRepository.deductIfSufficient(userId, "openai", 1) } returns false
        every { balanceRepository.getRequestsRemaining(userId, "openai") } returns 0

        val result = service.checkAndConsume(userId, "gpt-4o-mini")

        assertFalse(result.allowed)
        assertEquals(DenialReason.NO_SUBSCRIPTION, result.denialReason)
        assertEquals(0, result.availableRequests)
        assertEquals(1, result.modelCost)
    }

    @Test
    fun `checkAndConsume returns resetAt when free quota just exhausted`() {
        val quota = quota(remaining = 1, nextResetAt = future)
        every { freeQuotaRepository.findByUserIdAndProviderForUpdate(userId, "openai") } returns
            quota

        val result = service.checkAndConsume(userId, "gpt-4o-mini")

        assertTrue(result.allowed)
        assertEquals(future, result.resetAt)
    }

    @Test
    fun `checkAndConsume does not return resetAt when free quota not exhausted`() {
        val quota = quota(remaining = 5)
        every { freeQuotaRepository.findByUserIdAndProviderForUpdate(userId, "openai") } returns
            quota

        val result = service.checkAndConsume(userId, "gpt-4o-mini")

        assertTrue(result.allowed)
        assertEquals(null, result.resetAt)
    }

    @Test
    fun `checkAndConsume resets free quota and allows when reset period has passed`() {
        val expired = LocalDateTime.now().minusMinutes(1)
        val quota = quota(remaining = 0, nextResetAt = expired)
        every { freeQuotaRepository.findByUserIdAndProviderForUpdate(userId, "openai") } returns
            quota

        val result = service.checkAndConsume(userId, "gpt-4o-mini")

        assertTrue(result.allowed)
        assertEquals(9, quota.requestsRemaining)
        assertEquals(1, result.freeConsumed)
    }

    @ParameterizedTest(name = "freeConsumed={0}, paidConsumed={1}")
    @CsvSource(
        "3, 0",
        "0, 3",
        "1, 2",
    )
    fun `refund restores each portion to its original bucket`(
        freeConsumed: Int,
        paidConsumed: Int,
    ) {
        if (freeConsumed > 0) {
            justRun { freeQuotaRepository.addRequests(userId, "openai", freeConsumed) }
        }
        if (paidConsumed > 0) {
            justRun { balanceRepository.addRequests(userId, "openai", paidConsumed) }
        }

        service.refund(userId, "gpt-4o", freeConsumed = freeConsumed, paidConsumed = paidConsumed)

        if (freeConsumed > 0) {
            verify { freeQuotaRepository.addRequests(userId, "openai", freeConsumed) }
        } else {
            verify(exactly = 0) { freeQuotaRepository.addRequests(any(), any(), any()) }
        }
        if (paidConsumed > 0) {
            verify { balanceRepository.addRequests(userId, "openai", paidConsumed) }
        } else {
            verify(exactly = 0) { balanceRepository.addRequests(any(), any(), any()) }
        }
    }

    @Test
    fun `refund does nothing when model not in config`() {
        service.refund(userId, "unknown-model", freeConsumed = 1, paidConsumed = 1)

        verify(exactly = 0) { freeQuotaRepository.addRequests(any(), any(), any()) }
        verify(exactly = 0) { balanceRepository.addRequests(any(), any(), any()) }
    }

    @Test
    fun `refund does not touch free quota when freeConsumed is zero`() {
        justRun { balanceRepository.addRequests(userId, "openai", 1) }

        service.refund(userId, "gpt-4o-mini", freeConsumed = 0, paidConsumed = 1)

        verify(exactly = 0) { freeQuotaRepository.addRequests(any(), any(), any()) }
    }
}
