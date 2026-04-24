package com.xeno.subpilot.payment.unittests.service

import com.xeno.subpilot.payment.client.YooKassaClient
import com.xeno.subpilot.payment.dto.PlanDetails
import com.xeno.subpilot.payment.dto.YooKassaResult
import com.xeno.subpilot.payment.dto.kafka.YooKassaWebhookEvent
import com.xeno.subpilot.payment.dto.kafka.YooKassaWebhookPayment
import com.xeno.subpilot.payment.entity.OutboxPaymentEvent
import com.xeno.subpilot.payment.entity.Payment
import com.xeno.subpilot.payment.entity.PaymentStatus
import com.xeno.subpilot.payment.repository.OutboxPaymentEventJpaRepository
import com.xeno.subpilot.payment.repository.PaymentJpaRepository
import com.xeno.subpilot.payment.service.YooKassaPaymentService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tools.jackson.databind.json.JsonMapper

import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(MockKExtension::class)
class YooKassaPaymentServiceTest {

    @MockK lateinit var paymentJpaRepository: PaymentJpaRepository

    @MockK lateinit var yooKassaClient: YooKassaClient

    @MockK lateinit var outboxRepository: OutboxPaymentEventJpaRepository

    private val objectMapper = JsonMapper.builder().build()
    private val fixedClock: Clock =
        Clock.fixed(
            Instant.parse("2025-01-15T10:00:00Z"),
            ZoneOffset.UTC,
        )

    private lateinit var service: YooKassaPaymentService

    companion object {
        const val USER_ID = 42L
        const val PLAN_ID = "openai-basic"
        val PLAN = PlanDetails(price = BigDecimal("199.00"), currency = "RUB")
        val PAYMENT_ID: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val YOOKASSA_PAYMENT_ID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
        const val CONFIRMATION_URL = "https://yookassa.ru/checkout/payments/test"
        const val EVENT_SUCCEEDED = "payment.succeeded"
        const val EVENT_CANCELED = "payment.canceled"
    }

    @BeforeEach
    fun setUp() {
        service =
            YooKassaPaymentService(
                paymentJpaRepository = paymentJpaRepository,
                yooKassaClient = yooKassaClient,
                outboxPaymentEventJpaRepository = outboxRepository,
                objectMapper = objectMapper,
                clock = fixedClock,
            )
    }

    @Test
    fun `createPayment returns confirmation URL from YooKassa`() {
        givenSaveReturnsPaymentWithId()
        every { yooKassaClient.createPayment(any(), any(), any(), any()) } returns yooKassaResult()

        val result = service.createPayment(USER_ID, PLAN_ID, bonusPointsToApply = 0, plan = PLAN)

        assertEquals(CONFIRMATION_URL, result.confirmationUrl)
    }

    @Test
    fun `createPayment saves full price when no bonus applied`() {
        val savedPayment = slot<Payment>()
        every { paymentJpaRepository.save(capture(savedPayment)) } answers
            { savedPaymentWithId(savedPayment.captured) }
        every { yooKassaClient.createPayment(any(), any(), any(), any()) } returns yooKassaResult()

        service.createPayment(USER_ID, PLAN_ID, bonusPointsToApply = 0, plan = PLAN)

        assertEquals(BigDecimal("199.00"), savedPayment.captured.amount)
        assertEquals(0L, savedPayment.captured.bonusPointsUsed)
        assertEquals(USER_ID, savedPayment.captured.userId)
        assertEquals(PLAN_ID, savedPayment.captured.planId)
        assertEquals("RUB", savedPayment.captured.currency)
    }

    @ParameterizedTest(name = "bonus={0} → amount={1}, bonusUsed={2}")
    @CsvSource(
        "50,  149.00, 50",
        "199, 0.00,   199",
        "300, 0.00,   199",
    )
    fun `createPayment applies bonus discount correctly and caps at plan price`(
        bonusPoints: Long,
        expectedAmount: String,
        expectedBonusUsed: Long,
    ) {
        val savedPayment = slot<Payment>()
        every { paymentJpaRepository.save(capture(savedPayment)) } answers
            { savedPaymentWithId(savedPayment.captured) }
        every { yooKassaClient.createPayment(any(), any(), any(), any()) } returns yooKassaResult()

        service.createPayment(USER_ID, PLAN_ID, bonusPointsToApply = bonusPoints, plan = PLAN)

        assertEquals(0, BigDecimal(expectedAmount).compareTo(savedPayment.captured.amount))
        assertEquals(expectedBonusUsed, savedPayment.captured.bonusPointsUsed)
    }

    @Test
    fun `createPayment sets yookassaPaymentId on entity after YooKassa call`() {
        val savedPayment = slot<Payment>()
        var persistedPayment: Payment? = null
        every { paymentJpaRepository.save(capture(savedPayment)) } answers {
            savedPaymentWithId(savedPayment.captured).also { persistedPayment = it }
        }
        every { yooKassaClient.createPayment(any(), any(), any(), any()) } returns yooKassaResult()

        service.createPayment(USER_ID, PLAN_ID, bonusPointsToApply = 0, plan = PLAN)

        assertEquals(YOOKASSA_PAYMENT_ID, persistedPayment?.yooKassaPaymentId)
    }

    @Test
    fun `handlePaymentWebhook ignores unknown yookassaPaymentId`() {
        every { paymentJpaRepository.findByYooKassaPaymentId(any()) } returns null

        service.handlePaymentWebhook(webhookEvent(EVENT_SUCCEEDED))

        verify(exactly = 0) { outboxRepository.save(any()) }
    }

    @Test
    fun `handlePaymentWebhook ignores non-succeeded events`() {
        every { paymentJpaRepository.findByYooKassaPaymentId(YOOKASSA_PAYMENT_ID) } returns
            pendingPayment()

        service.handlePaymentWebhook(webhookEvent(EVENT_CANCELED))

        verify(exactly = 0) { paymentJpaRepository.updateStatusIfPending(any(), any(), any()) }
        verify(exactly = 0) { outboxRepository.save(any()) }
    }

    @Test
    fun `handlePaymentWebhook updates payment status to SUCCEEDED on success`() {
        every { paymentJpaRepository.findByYooKassaPaymentId(YOOKASSA_PAYMENT_ID) } returns
            pendingPayment()
        every {
            paymentJpaRepository.updateStatusIfPending(PAYMENT_ID, PaymentStatus.SUCCEEDED, any())
        } returns 1
        every { outboxRepository.save(any()) } answers { firstArg() }

        service.handlePaymentWebhook(webhookEvent(EVENT_SUCCEEDED))

        verify {
            paymentJpaRepository.updateStatusIfPending(
                PAYMENT_ID,
                PaymentStatus.SUCCEEDED,
                any(),
            )
        }
    }

    @Test
    fun `handlePaymentWebhook writes unpublished outbox event on success`() {
        every { paymentJpaRepository.findByYooKassaPaymentId(YOOKASSA_PAYMENT_ID) } returns
            pendingPayment()
        every { paymentJpaRepository.updateStatusIfPending(any(), any(), any()) } returns 1
        val outboxSlot = slot<OutboxPaymentEvent>()
        every { outboxRepository.save(capture(outboxSlot)) } answers { outboxSlot.captured }

        service.handlePaymentWebhook(webhookEvent(EVENT_SUCCEEDED))

        with(outboxSlot.captured) {
            assertEquals("payment_succeeded", eventType)
            assertNull(publishedAt)
        }
    }

    @Test
    fun `handlePaymentWebhook outbox payload contains userId and planId`() {
        every { paymentJpaRepository.findByYooKassaPaymentId(YOOKASSA_PAYMENT_ID) } returns
            pendingPayment()
        every { paymentJpaRepository.updateStatusIfPending(any(), any(), any()) } returns 1
        val outboxSlot = slot<OutboxPaymentEvent>()
        every { outboxRepository.save(capture(outboxSlot)) } answers { outboxSlot.captured }

        service.handlePaymentWebhook(webhookEvent(EVENT_SUCCEEDED))

        val payload = outboxSlot.captured.payload
        assert(payload.contains(USER_ID.toString())) { "payload must contain userId" }
        assert(payload.contains(PLAN_ID)) { "payload must contain planId" }
    }

    @Test
    fun `handlePaymentWebhook is idempotent — no outbox event when already processed`() {
        every { paymentJpaRepository.findByYooKassaPaymentId(YOOKASSA_PAYMENT_ID) } returns
            pendingPayment()
        every { paymentJpaRepository.updateStatusIfPending(any(), any(), any()) } returns 0

        service.handlePaymentWebhook(webhookEvent(EVENT_SUCCEEDED))

        verify(exactly = 0) { outboxRepository.save(any()) }
    }

    private fun givenSaveReturnsPaymentWithId() {
        val captured = slot<Payment>()
        every { paymentJpaRepository.save(capture(captured)) } answers
            { savedPaymentWithId(captured.captured) }
    }

    private fun savedPaymentWithId(payment: Payment): Payment =
        Payment(
            id = PAYMENT_ID,
            userId = payment.userId,
            planId = payment.planId,
            yooKassaPaymentId = payment.yooKassaPaymentId,
            amount = payment.amount,
            currency = payment.currency,
            status = payment.status,
            bonusPointsUsed = payment.bonusPointsUsed,
            createdAt = payment.createdAt,
            updatedAt = payment.updatedAt,
        )

    private fun pendingPayment(): Payment =
        Payment(
            id = PAYMENT_ID,
            userId = USER_ID,
            planId = PLAN_ID,
            yooKassaPaymentId = YOOKASSA_PAYMENT_ID,
            amount = PLAN.price,
            currency = PLAN.currency,
            status = PaymentStatus.PENDING,
            createdAt = LocalDateTime.now(fixedClock),
            updatedAt = LocalDateTime.now(fixedClock),
        )

    private fun webhookEvent(eventType: String): YooKassaWebhookEvent =
        YooKassaWebhookEvent(
            event = eventType,
            payment = YooKassaWebhookPayment(id = YOOKASSA_PAYMENT_ID, status = "succeeded"),
        )

    private fun yooKassaResult(): YooKassaResult =
        YooKassaResult(yookassaPaymentId = YOOKASSA_PAYMENT_ID, confirmationUrl = CONFIRMATION_URL)
}
