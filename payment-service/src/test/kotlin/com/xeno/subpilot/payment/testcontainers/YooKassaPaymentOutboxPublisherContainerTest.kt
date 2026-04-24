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
import com.xeno.subpilot.payment.dto.PlanDetails
import com.xeno.subpilot.payment.dto.YooKassaResult
import com.xeno.subpilot.payment.dto.kafka.YooKassaWebhookEvent
import com.xeno.subpilot.payment.dto.kafka.YooKassaWebhookPayment
import com.xeno.subpilot.payment.repository.OutboxPaymentEventJpaRepository
import com.xeno.subpilot.payment.service.YooKassaPaymentService
import com.xeno.subpilot.payment.service.kafka.YooKassaPaymentOutboxPublisher
import io.mockk.every
import net.datafaker.Faker
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional

import java.math.BigDecimal
import java.time.Duration
import java.util.UUID

import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class YooKassaPaymentOutboxPublisherContainerTest {

    @MockkBean
    lateinit var yooKassaClient: YooKassaClient

    @Autowired lateinit var paymentService: YooKassaPaymentService

    @Autowired lateinit var outboxPublisher: YooKassaPaymentOutboxPublisher

    @Autowired lateinit var outboxRepository: OutboxPaymentEventJpaRepository

    private lateinit var kafkaConsumer: KafkaConsumer<String, String>

    companion object {
        private val faker = Faker()
        private val postgres = TestContainersConfiguration.postgres
        private val kafka = TestContainersConfiguration.kafka

        val PLAN = PlanDetails(price = BigDecimal("199.00"), currency = "RUB")
        const val PLAN_ID = "openai-basic"
        const val TOPIC = "payment_succeeded"
        const val CONFIRMATION_URL = "https://yookassa.ru/checkout/payments/test"

        @JvmStatic
        @DynamicPropertySource
        fun configure(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }
        }
    }

    @BeforeEach
    fun setUpConsumer() {
        kafkaConsumer =
            KafkaConsumer(
                mapOf(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG
                        to kafka.bootstrapServers,
                    ConsumerConfig.GROUP_ID_CONFIG
                        to "test-${UUID.randomUUID()}",
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG
                        to "latest",
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG
                        to StringDeserializer::class.java,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG
                        to StringDeserializer::class.java,
                ),
            )
        kafkaConsumer.subscribe(listOf(TOPIC))
        waitForPartitionAssignment()
    }

    @AfterEach
    fun tearDownConsumer() {
        kafkaConsumer.close()
    }

    @Test
    @Transactional
    fun `publish sends outbox events to Kafka topic`() {
        val userId = randomUserId()
        val yooKassaId = UUID.randomUUID()
        givenSucceededPayment(userId, yooKassaId)

        outboxPublisher.publish()

        assertTrue(
            hasKafkaRecordsWithin(Duration.ofSeconds(5)),
            "Kafka topic must contain published outbox events",
        )
    }

    @Test
    @Transactional
    fun `publish marks outbox events as published after sending`() {
        val userId = randomUserId()
        val yooKassaId = UUID.randomUUID()
        givenSucceededPayment(userId, yooKassaId)
        val countBefore = outboxRepository.findUnpublished(100).size

        outboxPublisher.publish()

        val countAfter = outboxRepository.findUnpublished(100).size
        assertTrue(
            countAfter < countBefore,
            "published events must not appear in unpublished queue",
        )
    }

    @Test
    fun `publish does not re-send already published events`() {
        outboxPublisher.publish()

        val records = kafkaConsumer.poll(Duration.ofSeconds(2))
        assertTrue(
            records.isEmpty,
            "re-publish run must produce no new Kafka messages",
        )
    }

    private fun givenSucceededPayment(
        userId: Long,
        yooKassaId: UUID,
    ) {
        every {
            yooKassaClient.createPayment(
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            YooKassaResult(yooKassaId, CONFIRMATION_URL)

        paymentService.createPayment(userId, PLAN_ID, bonusPointsToApply = 0, plan = PLAN)
        paymentService.handlePaymentWebhook(
            YooKassaWebhookEvent(
                event = "payment.succeeded",
                payment = YooKassaWebhookPayment(id = yooKassaId, status = "succeeded"),
            ),
        )
    }

    private fun waitForPartitionAssignment() {
        repeat(10) {
            kafkaConsumer.poll(Duration.ofMillis(200))
            if (kafkaConsumer.assignment().isNotEmpty()) {
                return
            }
        }
    }

    private fun hasKafkaRecordsWithin(timeout: Duration): Boolean {
        val deadline = System.nanoTime() + timeout.toNanos()
        while (System.nanoTime() < deadline) {
            if (!kafkaConsumer.poll(Duration.ofMillis(200)).isEmpty) {
                return true
            }
        }
        return false
    }

    private fun randomUserId(): Long = faker.number().numberBetween(100_000L, Long.MAX_VALUE)
}
