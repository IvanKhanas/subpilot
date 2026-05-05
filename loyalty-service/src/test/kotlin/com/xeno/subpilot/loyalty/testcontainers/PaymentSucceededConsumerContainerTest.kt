/*
 * Copyright 2026 Ivan Khanas
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
package com.xeno.subpilot.loyalty.testcontainers

import com.ninjasquad.springmockk.MockkBean
import com.xeno.subpilot.loyalty.client.SubscriptionClient
import com.xeno.subpilot.loyalty.entity.LoyaltyTransactionType
import com.xeno.subpilot.loyalty.properties.LoyaltyProperties
import com.xeno.subpilot.loyalty.repository.LoyaltyTransactionJpaRepository
import com.xeno.subpilot.loyalty.repository.UserLoyaltyBalanceJpaRepository
import net.datafaker.Faker
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.errors.TopicExistsException
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PaymentSucceededConsumerContainerTest {

    @MockkBean
    lateinit var subscriptionGrpcClient: SubscriptionClient

    @Autowired
    lateinit var loyaltyProperties: LoyaltyProperties

    @Autowired
    lateinit var balanceJpaRepository: UserLoyaltyBalanceJpaRepository

    @Autowired
    lateinit var transactionJpaRepository: LoyaltyTransactionJpaRepository

    private lateinit var producer: KafkaProducer<String, String>

    companion object {
        private val faker = Faker()
        private val postgres = TestContainersConfiguration.postgres
        private val kafka = TestContainersConfiguration.kafka

        const val PAYMENT_TOPIC = "payment_succeeded"
        const val PAYMENT_AMOUNT = "199.00"

        @JvmStatic
        @DynamicPropertySource
        fun configure(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }
            registry.add("spring.kafka.listener.auto-startup") { true }
        }
    }

    @BeforeEach
    fun setUp() {
        ensureTopicExists()
        producer =
            KafkaProducer(
                mapOf(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ),
            )
    }

    @AfterEach
    fun tearDown() {
        producer.close()
    }

    @Test
    fun `payment_succeeded credits cashback points and persists EARNED transaction`() {
        val userId = randomUserId()
        val paymentId = UUID.randomUUID()
        val expectedPoints = cashbackPoints(PAYMENT_AMOUNT)

        publishPaymentEvent(userId, paymentId)

        awaitBalance(userId, Duration.ofSeconds(15))

        assertEquals(expectedPoints, balanceJpaRepository.findPointsByUserId(userId))
        val transaction =
            transactionJpaRepository
                .findAll()
                .single { it.paymentId == paymentId && it.type == LoyaltyTransactionType.EARNED }
        assertEquals(expectedPoints, transaction.amount)
    }

    @Test
    fun `payment_succeeded is idempotent on duplicate delivery`() {
        val userId = randomUserId()
        val paymentId = UUID.randomUUID()
        val expectedPoints = cashbackPoints(PAYMENT_AMOUNT)

        publishPaymentEvent(userId, paymentId)
        publishPaymentEvent(userId, paymentId)

        awaitBalance(userId, Duration.ofSeconds(15))

        assertEquals(
            expectedPoints,
            balanceJpaRepository.findPointsByUserId(userId),
            "duplicate event must not double-credit points",
        )
        val earnedCount =
            transactionJpaRepository
                .findAll()
                .count { it.paymentId == paymentId && it.type == LoyaltyTransactionType.EARNED }
        assertEquals(1, earnedCount, "duplicate event must produce exactly one transaction record")
    }

    private fun publishPaymentEvent(
        userId: Long,
        paymentId: UUID,
    ) {
        producer
            .send(
                ProducerRecord(PAYMENT_TOPIC, paymentJson(userId, paymentId)),
            ).get(10, TimeUnit.SECONDS)
    }

    private fun paymentJson(
        userId: Long,
        paymentId: UUID,
    ) =
        """{"payment_id":"$paymentId","user_id":$userId,"plan_id":"openai-basic","amount":"$PAYMENT_AMOUNT","bonus_points_used":0}"""

    private fun awaitBalance(
        userId: Long,
        timeout: Duration,
    ) {
        val deadline = System.nanoTime() + timeout.toNanos()
        while (System.nanoTime() < deadline) {
            if (balanceJpaRepository.findPointsByUserId(userId) != null) return
            Thread.sleep(200)
        }
        error("Loyalty balance for user $userId not credited within $timeout")
    }

    private fun cashbackPoints(amount: String): Long =
        BigDecimal(amount)
            .multiply(loyaltyProperties.cashbackRate)
            .setScale(0, RoundingMode.FLOOR)
            .toLong()

    private fun randomUserId(): Long = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)

    private fun ensureTopicExists() {
        AdminClient
            .create(mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers))
            .use { admin ->
                try {
                    admin
                        .createTopics(
                            listOf(NewTopic(PAYMENT_TOPIC, 1, 1)),
                        ).all()
                        .get(10, TimeUnit.SECONDS)
                } catch (e: ExecutionException) {
                    if (e.cause !is TopicExistsException) throw e
                }
            }
    }
}
