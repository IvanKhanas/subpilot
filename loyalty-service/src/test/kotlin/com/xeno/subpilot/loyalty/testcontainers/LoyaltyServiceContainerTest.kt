package com.xeno.subpilot.loyalty.testcontainers

import com.ninjasquad.springmockk.MockkBean
import com.xeno.subpilot.loyalty.client.SubscriptionGrpcClient
import com.xeno.subpilot.loyalty.dto.SpendDenialReason
import com.xeno.subpilot.loyalty.dto.SpendResult
import com.xeno.subpilot.loyalty.dto.kafka.PaymentSucceededEvent
import com.xeno.subpilot.loyalty.entity.LoyaltyTransactionType
import com.xeno.subpilot.loyalty.properties.LoyaltyProperties
import com.xeno.subpilot.loyalty.repository.LoyaltyTransactionJpaRepository
import com.xeno.subpilot.loyalty.repository.UserLoyaltyBalanceJpaRepository
import com.xeno.subpilot.loyalty.service.LoyaltyService
import com.xeno.subpilot.proto.subscription.v1.planInfo
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

import kotlin.test.assertEquals
import kotlin.test.assertIs

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class LoyaltyServiceContainerTest {

    @MockkBean
    lateinit var subscriptionGrpcClient: SubscriptionGrpcClient

    @Autowired
    lateinit var loyaltyService: LoyaltyService

    @Autowired
    lateinit var loyaltyProperties: LoyaltyProperties

    @Autowired
    lateinit var userLoyaltyBalanceJpaRepository: UserLoyaltyBalanceJpaRepository

    @Autowired
    lateinit var loyaltyTransactionJpaRepository: LoyaltyTransactionJpaRepository

    companion object {
        private val faker = Faker()
        private val postgres = TestContainersConfiguration.postgres
        private val kafka = TestContainersConfiguration.kafka
        private const val PLAN_ID = "openai-basic"

        @JvmStatic
        @DynamicPropertySource
        fun configure(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }
        }
    }

    @Test
    fun `earn credits cashback and persists EARNED transaction`() {
        val userId = randomUserId()
        val paymentId = UUID.randomUUID()
        val expectedPoints = cashbackPoints("199.99")

        loyaltyService.earn(event(userId = userId, paymentId = paymentId, amount = "199.99"))

        assertEquals(expectedPoints, userLoyaltyBalanceJpaRepository.findPointsByUserId(userId))
        val transaction =
            loyaltyTransactionJpaRepository
                .findAll()
                .single { it.paymentId == paymentId && it.type == LoyaltyTransactionType.EARNED }
        assertEquals(expectedPoints, transaction.amount)
    }

    @Test
    fun `earn is idempotent for duplicate payment event`() {
        val userId = randomUserId()
        val paymentId = UUID.randomUUID()
        val expectedPoints = cashbackPoints("100.00")
        val event = event(userId = userId, paymentId = paymentId, amount = "100.00")

        loyaltyService.earn(event)
        loyaltyService.earn(event)

        assertEquals(expectedPoints, userLoyaltyBalanceJpaRepository.findPointsByUserId(userId))
        val earnedCount =
            loyaltyTransactionJpaRepository
                .findAll()
                .count { it.paymentId == paymentId && it.type == LoyaltyTransactionType.EARNED }
        assertEquals(1, earnedCount)
    }

    @Test
    fun `earn deducts bonus points used and persists both transaction types`() {
        val userId = randomUserId()
        val paymentId = UUID.randomUUID()
        val expectedEarnedPoints = cashbackPoints("100.00")
        userLoyaltyBalanceJpaRepository.upsertAdd(userId, 100)

        loyaltyService.earn(
            event(
                userId = userId,
                paymentId = paymentId,
                amount = "100.00",
                bonusPointsUsed = 20,
            ),
        )

        assertEquals(
            100 + expectedEarnedPoints - 20,
            userLoyaltyBalanceJpaRepository.findPointsByUserId(userId),
        )
        val transactions =
            loyaltyTransactionJpaRepository
                .findAll()
                .filter { it.paymentId == paymentId }
        assertEquals(2, transactions.size)
        assertEquals(
            expectedEarnedPoints,
            transactions
                .single {
                    it.type ==
                        LoyaltyTransactionType.EARNED
                }.amount,
        )
        assertEquals(20L, transactions.single { it.type == LoyaltyTransactionType.SPENT }.amount)
    }

    @Test
    fun `spend denies when points are insufficient`() {
        val userId = randomUserId()
        val idempotencyKey = UUID.randomUUID()
        userLoyaltyBalanceJpaRepository.upsertAdd(userId, 50)
        every { subscriptionGrpcClient.getPlanInfo(PLAN_ID) } returns plan(price = "199.00")

        val result = loyaltyService.spend(userId, PLAN_ID, idempotencyKey)

        val denied = assertIs<SpendResult.Denied>(result)
        assertEquals(SpendDenialReason.INSUFFICIENT_POINTS, denied.reason)
        assertEquals(50L, userLoyaltyBalanceJpaRepository.findPointsByUserId(userId))
        assertEquals(
            0,
            loyaltyTransactionJpaRepository
                .findAll()
                .count {
                    it.paymentId == idempotencyKey && it.type == LoyaltyTransactionType.SPENT
                },
        )
    }

    @ParameterizedTest(name = "price={0} -> deducted points={1}")
    @CsvSource(
        "199.00, 199",
        "199.01, 200",
        "199.99, 200",
    )
    fun `spend rounds price up, deducts points and activates subscription`(
        price: String,
        expectedDeduction: Long,
    ) {
        val userId = randomUserId()
        val idempotencyKey = UUID.randomUUID()
        userLoyaltyBalanceJpaRepository.upsertAdd(userId, 1000)
        every { subscriptionGrpcClient.getPlanInfo(PLAN_ID) } returns plan(price = price)
        justRun { subscriptionGrpcClient.activateSubscription(userId, PLAN_ID, idempotencyKey) }

        val result = loyaltyService.spend(userId, PLAN_ID, idempotencyKey)

        assertIs<SpendResult.Success>(result)
        assertEquals(
            1000 - expectedDeduction,
            userLoyaltyBalanceJpaRepository.findPointsByUserId(userId),
        )
        verify { subscriptionGrpcClient.activateSubscription(userId, PLAN_ID, idempotencyKey) }
        val spent =
            loyaltyTransactionJpaRepository
                .findAll()
                .single {
                    it.paymentId == idempotencyKey && it.type == LoyaltyTransactionType.SPENT
                }
        assertEquals(expectedDeduction, spent.amount)
    }

    @Test
    fun `spend is idempotent for duplicate idempotency key`() {
        val userId = randomUserId()
        val idempotencyKey = UUID.randomUUID()
        userLoyaltyBalanceJpaRepository.upsertAdd(userId, 500)
        every { subscriptionGrpcClient.getPlanInfo(PLAN_ID) } returns plan(price = "199.00")
        justRun { subscriptionGrpcClient.activateSubscription(userId, PLAN_ID, idempotencyKey) }

        loyaltyService.spend(userId, PLAN_ID, idempotencyKey)
        loyaltyService.spend(userId, PLAN_ID, idempotencyKey)

        assertEquals(301L, userLoyaltyBalanceJpaRepository.findPointsByUserId(userId))
        verify(
            exactly = 1,
        ) { subscriptionGrpcClient.activateSubscription(userId, PLAN_ID, idempotencyKey) }
        val spentCount =
            loyaltyTransactionJpaRepository
                .findAll()
                .count { it.paymentId == idempotencyKey && it.type == LoyaltyTransactionType.SPENT }
        assertEquals(1, spentCount)
    }

    private fun randomUserId(): Long = faker.number().numberBetween(100_000L, Long.MAX_VALUE)

    private fun cashbackPoints(amount: String): Long =
        BigDecimal(amount)
            .multiply(loyaltyProperties.cashbackRate)
            .setScale(0, RoundingMode.FLOOR)
            .toLong()

    private fun event(
        userId: Long,
        paymentId: UUID,
        amount: String,
        bonusPointsUsed: Long = 0,
    ): PaymentSucceededEvent =
        PaymentSucceededEvent(
            paymentId = paymentId,
            userId = userId,
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
