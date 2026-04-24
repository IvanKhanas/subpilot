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
package com.xeno.subpilot.payment.testcontainers

import com.ninjasquad.springmockk.MockkBean
import com.xeno.subpilot.payment.client.YooKassaClient
import com.xeno.subpilot.payment.dto.PaymentResult
import com.xeno.subpilot.payment.dto.PlanDetails
import com.xeno.subpilot.payment.dto.YooKassaResult
import com.xeno.subpilot.payment.dto.kafka.YooKassaWebhookEvent
import com.xeno.subpilot.payment.dto.kafka.YooKassaWebhookPayment
import com.xeno.subpilot.payment.entity.PaymentStatus
import com.xeno.subpilot.payment.repository.OutboxPaymentEventJpaRepository
import com.xeno.subpilot.payment.repository.PaymentJpaRepository
import com.xeno.subpilot.payment.service.YooKassaPaymentService
import io.mockk.every
import jakarta.persistence.EntityManager
import net.datafaker.Faker
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional

import java.math.BigDecimal
import java.util.UUID

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class YooKassaPaymentServiceContainerTest {

    @MockkBean
    lateinit var yooKassaClient: YooKassaClient

    @MockkBean(relaxed = true)
    @Suppress("unused")
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Autowired lateinit var paymentService: YooKassaPaymentService

    @Autowired lateinit var paymentRepository: PaymentJpaRepository

    @Autowired lateinit var outboxRepository: OutboxPaymentEventJpaRepository

    @Autowired lateinit var entityManager: EntityManager

    companion object {
        private val faker = Faker()
        private val postgres = TestContainersConfiguration.postgres

        val PLAN = PlanDetails(price = BigDecimal("199.00"), currency = "RUB")
        const val PLAN_ID = "openai-basic"
        const val CONFIRMATION_URL = "https://yookassa.ru/checkout/payments/test"
        const val EVENT_SUCCEEDED = "payment.succeeded"
        const val EVENT_CANCELED = "payment.canceled"

        @JvmStatic
        @DynamicPropertySource
        fun configure(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.kafka.bootstrap-servers") { "localhost:9999" }
        }
    }

    @Test
    fun `createPayment persists payment record with PENDING status`() {
        val userId = randomUserId()
        givenYooKassaReturnsPayment()

        val result =
            paymentService.createPayment(
                userId,
                PLAN_ID,
                bonusPointsToApply = 0,
                plan = PLAN,
            )

        val saved = paymentRepository.findById(UUID.fromString(result.paymentId)).orElse(null)
        assertNotNull(saved)
        assertEquals(userId, saved.userId)
        assertEquals(PLAN_ID, saved.planId)
        assertEquals(PaymentStatus.PENDING, saved.status)
        assertEquals(BigDecimal("199.00"), saved.amount)
        assertEquals("RUB", saved.currency)
    }

    @Test
    fun `createPayment persists discounted amount when bonus applied`() {
        val userId = randomUserId()
        givenYooKassaReturnsPayment()

        val result =
            paymentService.createPayment(
                userId,
                PLAN_ID,
                bonusPointsToApply = 50,
                plan = PLAN,
            )

        val saved = paymentRepository.findById(UUID.fromString(result.paymentId)).orElse(null)!!
        assertEquals(0, BigDecimal("149.00").compareTo(saved.amount))
        assertEquals(50L, saved.bonusPointsUsed)
    }

    @Test
    fun `createPayment links yookassaPaymentId to saved record`() {
        val userId = randomUserId()
        val expectedYooKassaId = UUID.randomUUID()
        every { yooKassaClient.createPayment(any(), any(), any(), any()) } returns
            YooKassaResult(expectedYooKassaId, CONFIRMATION_URL)

        val result =
            paymentService.createPayment(
                userId,
                PLAN_ID,
                bonusPointsToApply = 0,
                plan = PLAN,
            )

        val saved = paymentRepository.findById(UUID.fromString(result.paymentId)).orElse(null)!!
        assertEquals(expectedYooKassaId, saved.yooKassaPaymentId)
    }

    @Test
    fun `handlePaymentWebhook transitions payment to SUCCEEDED and writes outbox event`() {
        val userId = randomUserId()
        val yooKassaId = UUID.randomUUID()
        val paymentResult = createPendingPayment(userId, yooKassaId)
        val paymentId = UUID.fromString(paymentResult.paymentId)

        paymentService.handlePaymentWebhook(webhookEvent(yooKassaId, EVENT_SUCCEEDED))

        entityManager.clear()
        val payment = paymentRepository.findById(paymentId).orElse(null)!!
        assertEquals(PaymentStatus.SUCCEEDED, payment.status)

        val outboxEvents = outboxRepository.findUnpublished(10)
        assertTrue(outboxEvents.any { it.payload.contains(userId.toString()) })
    }

    @Test
    fun `handlePaymentWebhook is idempotent on duplicate webhook`() {
        val userId = randomUserId()
        val yooKassaId = UUID.randomUUID()
        createPendingPayment(userId, yooKassaId)
        val event = webhookEvent(yooKassaId, EVENT_SUCCEEDED)

        paymentService.handlePaymentWebhook(event)
        paymentService.handlePaymentWebhook(event)

        val outboxCount =
            outboxRepository.findUnpublished(10).count {
                it.payload.contains(userId.toString())
            }
        assertEquals(1, outboxCount)
    }

    @Test
    fun `handlePaymentWebhook ignores non-succeeded events`() {
        val userId = randomUserId()
        val yooKassaId = UUID.randomUUID()
        val paymentResult = createPendingPayment(userId, yooKassaId)
        val paymentId = UUID.fromString(paymentResult.paymentId)

        paymentService.handlePaymentWebhook(webhookEvent(yooKassaId, EVENT_CANCELED))

        val payment = paymentRepository.findById(paymentId).orElse(null)!!
        assertEquals(PaymentStatus.PENDING, payment.status)
        assertNull(
            outboxRepository.findUnpublished(10).firstOrNull {
                it.payload.contains(userId.toString())
            },
        )
    }

    @Test
    fun `handlePaymentWebhook ignores unknown payment`() {
        val countBefore = outboxRepository.findUnpublished(100).size

        paymentService.handlePaymentWebhook(webhookEvent(UUID.randomUUID(), EVENT_SUCCEEDED))

        assertEquals(countBefore, outboxRepository.findUnpublished(100).size)
    }

    private fun givenYooKassaReturnsPayment() {
        every { yooKassaClient.createPayment(any(), any(), any(), any()) } returns
            YooKassaResult(UUID.randomUUID(), CONFIRMATION_URL)
    }

    private fun createPendingPayment(
        userId: Long,
        yooKassaId: UUID,
    ): PaymentResult {
        every { yooKassaClient.createPayment(any(), any(), any(), any()) } returns
            YooKassaResult(yooKassaId, CONFIRMATION_URL)
        return paymentService.createPayment(userId, PLAN_ID, bonusPointsToApply = 0, plan = PLAN)
    }

    private fun webhookEvent(
        yooKassaId: UUID,
        eventType: String,
    ): YooKassaWebhookEvent =
        YooKassaWebhookEvent(
            event = eventType,
            payment = YooKassaWebhookPayment(id = yooKassaId, status = "succeeded"),
        )

    private fun randomUserId(): Long = faker.number().numberBetween(100_000L, Long.MAX_VALUE)
}
