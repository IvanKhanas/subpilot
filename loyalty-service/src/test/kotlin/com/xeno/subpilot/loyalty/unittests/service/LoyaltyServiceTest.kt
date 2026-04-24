package com.xeno.subpilot.loyalty.unittests.service

import com.xeno.subpilot.loyalty.client.SubscriptionGrpcClient
import com.xeno.subpilot.loyalty.dto.SpendDenialReason
import com.xeno.subpilot.loyalty.dto.SpendResult
import com.xeno.subpilot.loyalty.dto.kafka.PaymentSucceededEvent
import com.xeno.subpilot.loyalty.properties.LoyaltyProperties
import com.xeno.subpilot.loyalty.repository.LoyaltyTransactionJpaRepository
import com.xeno.subpilot.loyalty.repository.UserLoyaltyBalanceJpaRepository
import com.xeno.subpilot.loyalty.service.LoyaltyService
import com.xeno.subpilot.proto.subscription.v1.planInfo
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

import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

import kotlin.test.assertEquals
import kotlin.test.assertIs

@ExtendWith(MockKExtension::class)
class LoyaltyServiceTest {

    @MockK
    lateinit var loyaltyTransactionJpaRepository: LoyaltyTransactionJpaRepository

    @MockK
    lateinit var userLoyaltyBalanceJpaRepository: UserLoyaltyBalanceJpaRepository

    @MockK
    lateinit var subscriptionGrpcClient: SubscriptionGrpcClient

    private val fixedClock: Clock =
        Clock.fixed(
            Instant.parse("2026-01-10T10:00:00Z"),
            ZoneOffset.UTC,
        )
    private val now: LocalDateTime = LocalDateTime.ofInstant(fixedClock.instant(), ZoneOffset.UTC)

    private lateinit var service: LoyaltyService

    companion object {
        const val USER_ID = 42L
        const val PLAN_ID = "openai-basic"
    }

    @BeforeEach
    fun setUp() {
        service =
            LoyaltyService(
                loyaltyProperties = LoyaltyProperties(cashbackRate = BigDecimal("0.08")),
                loyaltyTransactionJpaRepository = loyaltyTransactionJpaRepository,
                userLoyaltyBalanceJpaRepository = userLoyaltyBalanceJpaRepository,
                subscriptionGrpcClient = subscriptionGrpcClient,
                clock = fixedClock,
            )
        justRun { userLoyaltyBalanceJpaRepository.upsertAdd(any(), any()) }
        justRun { subscriptionGrpcClient.activateSubscription(any(), any(), any()) }
    }

    @ParameterizedTest(name = "amount={0} -> cashback points = 0, earn skipped")
    @CsvSource(
        "0.00",
        "0.99",
        "12.49",
    )
    fun `earn skips when cashback points are zero`(amount: String) {
        service.earn(event(amount = amount))

        verify(exactly = 0) {
            loyaltyTransactionJpaRepository.insertEarnedIfAbsent(any(), any(), any(), any())
        }
        verify(exactly = 0) { userLoyaltyBalanceJpaRepository.upsertAdd(any(), any()) }
    }

    @ParameterizedTest(name = "amount={0} -> points={1}")
    @CsvSource(
        "12.50, 1",
        "100.00, 8",
        "199.99, 15",
    )
    fun `earn credits rounded down cashback points`(
        amount: String,
        expectedPoints: Long,
    ) {
        val paymentId = UUID.randomUUID()
        every {
            loyaltyTransactionJpaRepository.insertEarnedIfAbsent(
                USER_ID,
                expectedPoints,
                paymentId,
                now,
            )
        } returns 1

        service.earn(event(amount = amount, paymentId = paymentId))

        verify {
            loyaltyTransactionJpaRepository.insertEarnedIfAbsent(
                USER_ID,
                expectedPoints,
                paymentId,
                now,
            )
        }
        verify { userLoyaltyBalanceJpaRepository.upsertAdd(USER_ID, expectedPoints) }
    }

    @Test
    fun `earn is idempotent and does not update balance for duplicate payment`() {
        val paymentId = UUID.randomUUID()
        every {
            loyaltyTransactionJpaRepository.insertEarnedIfAbsent(any(), any(), paymentId, any())
        } returns 0

        service.earn(event(amount = "100.00", paymentId = paymentId, bonusPointsUsed = 20))

        verify(exactly = 0) { userLoyaltyBalanceJpaRepository.upsertAdd(any(), any()) }
        verify(exactly = 0) {
            loyaltyTransactionJpaRepository.insertSpentIfAbsent(any(), any(), any(), any())
        }
    }

    @Test
    fun `earn deducts bonus points used when spent transaction is inserted`() {
        val paymentId = UUID.randomUUID()
        every {
            loyaltyTransactionJpaRepository.insertEarnedIfAbsent(
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            1
        every {
            loyaltyTransactionJpaRepository.insertSpentIfAbsent(USER_ID, 20, paymentId, now)
        } returns 1
        every { userLoyaltyBalanceJpaRepository.deductIfSufficient(USER_ID, 20) } returns 1

        service.earn(event(amount = "100.00", paymentId = paymentId, bonusPointsUsed = 20))

        verify { loyaltyTransactionJpaRepository.insertSpentIfAbsent(USER_ID, 20, paymentId, now) }
        verify { userLoyaltyBalanceJpaRepository.deductIfSufficient(USER_ID, 20) }
    }

    @Test
    fun `earn does not deduct bonus points when spent transaction already exists`() {
        val paymentId = UUID.randomUUID()
        every {
            loyaltyTransactionJpaRepository.insertEarnedIfAbsent(
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            1
        every {
            loyaltyTransactionJpaRepository.insertSpentIfAbsent(any(), any(), paymentId, any())
        } returns 0

        service.earn(event(amount = "100.00", paymentId = paymentId, bonusPointsUsed = 20))

        verify(exactly = 0) { userLoyaltyBalanceJpaRepository.deductIfSufficient(any(), any()) }
    }

    @Test
    fun `earn still completes when bonus deduction cannot be applied`() {
        every {
            loyaltyTransactionJpaRepository.insertEarnedIfAbsent(
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            1
        every {
            loyaltyTransactionJpaRepository.insertSpentIfAbsent(
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            1
        every { userLoyaltyBalanceJpaRepository.deductIfSufficient(USER_ID, 20) } returns 0

        service.earn(event(amount = "100.00", bonusPointsUsed = 20))

        verify { userLoyaltyBalanceJpaRepository.deductIfSufficient(USER_ID, 20) }
    }

    @Test
    fun `getBalance returns zero when user has no row`() {
        every { userLoyaltyBalanceJpaRepository.findPointsByUserId(USER_ID) } returns null

        assertEquals(0L, service.getBalance(USER_ID))
    }

    @ParameterizedTest(name = "stored balance={0}")
    @CsvSource(
        "1",
        "15",
        "999",
    )
    fun `getBalance returns stored points`(storedPoints: Long) {
        every { userLoyaltyBalanceJpaRepository.findPointsByUserId(USER_ID) } returns storedPoints

        assertEquals(storedPoints, service.getBalance(USER_ID))
    }

    @Test
    fun `spend denies when plan does not exist`() {
        every { subscriptionGrpcClient.getPlanInfo(PLAN_ID) } returns null

        val result = service.spend(USER_ID, PLAN_ID, UUID.randomUUID())

        val denied = assertIs<SpendResult.Denied>(result)
        assertEquals(SpendDenialReason.UNKNOWN_PLAN, denied.reason)
        verify(exactly = 0) { userLoyaltyBalanceJpaRepository.deductIfSufficient(any(), any()) }
        verify(exactly = 0) {
            loyaltyTransactionJpaRepository.insertSpentIfAbsent(any(), any(), any(), any())
        }
    }

    @Test
    fun `spend denies when points are insufficient`() {
        every { subscriptionGrpcClient.getPlanInfo(PLAN_ID) } returns plan(price = "199.00")
        every { userLoyaltyBalanceJpaRepository.deductIfSufficient(USER_ID, 199) } returns 0

        val result = service.spend(USER_ID, PLAN_ID, UUID.randomUUID())

        val denied = assertIs<SpendResult.Denied>(result)
        assertEquals(SpendDenialReason.INSUFFICIENT_POINTS, denied.reason)
        verify(exactly = 0) {
            loyaltyTransactionJpaRepository.insertSpentIfAbsent(any(), any(), any(), any())
        }
    }

    @ParameterizedTest(name = "price={0} -> ceil={1}")
    @CsvSource(
        "1.00, 1",
        "199.00, 199",
        "199.01, 200",
        "199.99, 200",
    )
    fun `spend rounds plan price up before deduction`(
        price: String,
        expectedPoints: Long,
    ) {
        val idempotencyKey = UUID.randomUUID()
        every { subscriptionGrpcClient.getPlanInfo(PLAN_ID) } returns plan(price = price)
        every {
            userLoyaltyBalanceJpaRepository.deductIfSufficient(
                USER_ID,
                expectedPoints,
            )
        } returns
            1
        every {
            loyaltyTransactionJpaRepository.insertSpentIfAbsent(
                USER_ID,
                expectedPoints,
                idempotencyKey,
                now,
            )
        } returns 1

        val result = service.spend(USER_ID, PLAN_ID, idempotencyKey)

        assertIs<SpendResult.Success>(result)
        verify { userLoyaltyBalanceJpaRepository.deductIfSufficient(USER_ID, expectedPoints) }
        verify {
            loyaltyTransactionJpaRepository.insertSpentIfAbsent(
                USER_ID,
                expectedPoints,
                idempotencyKey,
                now,
            )
        }
        verify { subscriptionGrpcClient.activateSubscription(USER_ID, PLAN_ID, idempotencyKey) }
    }

    @Test
    fun `spend refunds points and skips activation on duplicate spent transaction`() {
        val idempotencyKey = UUID.randomUUID()
        every { subscriptionGrpcClient.getPlanInfo(PLAN_ID) } returns plan(price = "199.00")
        every { userLoyaltyBalanceJpaRepository.deductIfSufficient(USER_ID, 199) } returns 1
        every {
            loyaltyTransactionJpaRepository.insertSpentIfAbsent(USER_ID, 199, idempotencyKey, now)
        } returns 0

        val result = service.spend(USER_ID, PLAN_ID, idempotencyKey)

        assertIs<SpendResult.Success>(result)
        verify { userLoyaltyBalanceJpaRepository.upsertAdd(USER_ID, 199) }
        verify(exactly = 0) { subscriptionGrpcClient.activateSubscription(any(), any(), any()) }
    }

    private fun event(
        amount: String,
        paymentId: UUID = UUID.randomUUID(),
        bonusPointsUsed: Long = 0,
    ): PaymentSucceededEvent =
        PaymentSucceededEvent(
            paymentId = paymentId,
            userId = USER_ID,
            planId = PLAN_ID,
            amount = BigDecimal(amount),
            bonusPointsUsed = bonusPointsUsed,
        )

    private fun plan(price: String) =
        planInfo {
            planId = PLAN_ID
            provider = "openai"
            displayName = "OpenAI Basic"
            this.price = price
            currency = "RUB"
        }
}
