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
package com.xeno.subpilot.subscription.testcontainers

import com.xeno.subpilot.subscription.entity.SubscriptionUser
import com.xeno.subpilot.subscription.repository.SubscriptionUserJpaRepository
import com.xeno.subpilot.subscription.repository.UserRequestBalanceJpaRepository
import net.datafaker.Faker
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.TopicExistsException
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

import java.time.Duration
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PaymentSucceededConsumerContainerTest {

    @Autowired
    lateinit var userJpaRepository: SubscriptionUserJpaRepository

    @Autowired
    lateinit var balanceJpaRepository: UserRequestBalanceJpaRepository

    private lateinit var producer: KafkaProducer<String, String>
    private lateinit var activatedConsumer: KafkaConsumer<String, String>

    companion object {
        private val faker = Faker()
        private val postgres = TestContainersConfiguration.postgres
        private val kafka = TestContainersConfiguration.kafka

        const val PAYMENT_TOPIC = "payment_succeeded"
        const val ACTIVATED_TOPIC = "subscription_activated"
        const val PLAN_ID = "openai-basic"
        const val PLAN_PROVIDER = "openai"
        const val PLAN_REQUESTS = 100
        val ACTIVATED_PARTITION = TopicPartition(ACTIVATED_TOPIC, 0)

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
    fun setUp() {
        ensureTopicsExist()
        producer =
            KafkaProducer(
                mapOf(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ),
            )
        activatedConsumer =
            KafkaConsumer(
                mapOf(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ),
            )
        activatedConsumer.assign(listOf(ACTIVATED_PARTITION))
        waitForPartitionReady()
        activatedConsumer.seekToEnd(listOf(ACTIVATED_PARTITION))
    }

    @AfterEach
    fun tearDown() {
        producer.close()
        activatedConsumer.close()
    }

    @Test
    fun `payment_succeeded activates subscription and publishes subscription_activated`() {
        val userId = randomUserId()
        val paymentId = UUID.randomUUID()
        userJpaRepository.save(SubscriptionUser(userId = userId))
        val offsetBefore = currentActivatedOffset()

        publishPaymentEvent(userId, paymentId)

        assertTrue(
            waitForActivatedEvent(offsetBefore, Duration.ofSeconds(15)),
            "subscription_activated must be published after payment_succeeded consumed",
        )
        val balance = balanceJpaRepository.findByUserIdAndProvider(userId, PLAN_PROVIDER)
        assertNotNull(balance, "request balance must be credited after activation")
        assertEquals(PLAN_REQUESTS, balance.requestsRemaining)
    }

    @Test
    fun `payment_succeeded is idempotent on duplicate delivery`() {
        val userId = randomUserId()
        val paymentId = UUID.randomUUID()
        userJpaRepository.save(SubscriptionUser(userId = userId))

        publishPaymentEvent(userId, paymentId)
        publishPaymentEvent(userId, paymentId)

        waitForBalance(userId, PLAN_PROVIDER, Duration.ofSeconds(15))

        val balance = balanceJpaRepository.findByUserIdAndProvider(userId, PLAN_PROVIDER)
        assertNotNull(balance, "balance must exist after activation")
        assertEquals(
            PLAN_REQUESTS,
            balance.requestsRemaining,
            "duplicate event must not double-credit the balance",
        )
    }

    private fun publishPaymentEvent(
        userId: Long,
        paymentId: UUID,
    ) {
        producer
            .send(ProducerRecord(PAYMENT_TOPIC, paymentJson(userId, paymentId)))
            .get(10, TimeUnit.SECONDS)
    }

    private fun paymentJson(
        userId: Long,
        paymentId: UUID,
    ) = """{"payment_id":"$paymentId","user_id":$userId,"plan_id":"$PLAN_ID","amount":"199.00"}"""

    private fun waitForBalance(
        userId: Long,
        provider: String,
        timeout: Duration,
    ) {
        val deadline = System.nanoTime() + timeout.toNanos()
        while (System.nanoTime() < deadline) {
            if (balanceJpaRepository.findByUserIdAndProvider(userId, provider) != null) return
            Thread.sleep(300)
        }
        error("Balance for user $userId / $provider not credited within $timeout")
    }

    private fun waitForActivatedEvent(
        initialOffset: Long,
        timeout: Duration,
    ): Boolean {
        val deadline = System.nanoTime() + timeout.toNanos()
        while (System.nanoTime() < deadline) {
            activatedConsumer.poll(Duration.ofMillis(300))
            if (currentActivatedOffset() > initialOffset) return true
        }
        return false
    }

    private fun waitForPartitionReady() {
        val deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos()
        while (System.nanoTime() < deadline) {
            activatedConsumer.poll(Duration.ofMillis(200))
            val pos =
                runCatching {
                    activatedConsumer.position(
                        ACTIVATED_PARTITION,
                    )
                }.getOrDefault(-1L)
            if (pos >= 0L) return
        }
        error("Kafka consumer did not initialize for partition $ACTIVATED_PARTITION")
    }

    private fun currentActivatedOffset(): Long =
        activatedConsumer.endOffsets(listOf(ACTIVATED_PARTITION))[ACTIVATED_PARTITION]
            ?: error("No end offset for $ACTIVATED_PARTITION")

    private fun randomUserId(): Long = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)

    private fun ensureTopicsExist() {
        AdminClient
            .create(mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers))
            .use { admin ->
                listOf(PAYMENT_TOPIC, ACTIVATED_TOPIC).forEach { topic ->
                    try {
                        admin
                            .createTopics(
                                listOf(NewTopic(topic, 1, 1)),
                            ).all()
                            .get(10, TimeUnit.SECONDS)
                    } catch (e: ExecutionException) {
                        if (e.cause !is TopicExistsException) throw e
                    }
                }
            }
    }
}
