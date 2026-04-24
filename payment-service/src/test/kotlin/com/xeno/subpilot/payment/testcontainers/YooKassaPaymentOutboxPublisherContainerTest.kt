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
import com.xeno.subpilot.payment.entity.OutboxPaymentEvent
import com.xeno.subpilot.payment.repository.OutboxPaymentEventJpaRepository
import com.xeno.subpilot.payment.service.kafka.YooKassaPaymentOutboxPublisher
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.TopicExistsException
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class YooKassaPaymentOutboxPublisherContainerTest {

    @MockkBean
    lateinit var yooKassaClient: YooKassaClient

    @Autowired lateinit var outboxPublisher: YooKassaPaymentOutboxPublisher

    @Autowired lateinit var outboxRepository: OutboxPaymentEventJpaRepository

    private lateinit var kafkaConsumer: KafkaConsumer<String, String>

    companion object {
        private val postgres = TestContainersConfiguration.postgres
        private val kafka = TestContainersConfiguration.kafka

        const val TOPIC = "payment_succeeded"
        val TOPIC_PARTITION = TopicPartition(TOPIC, 0)

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
        ensureTopicExists()
        kafkaConsumer =
            KafkaConsumer(
                mapOf(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG
                        to kafka.bootstrapServers,
                    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG
                        to StringDeserializer::class.java,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG
                        to StringDeserializer::class.java,
                ),
            )
        kafkaConsumer.assign(listOf(TOPIC_PARTITION))
        waitForPartitionReady()
        kafkaConsumer.seekToEnd(listOf(TOPIC_PARTITION))
        outboxRepository.deleteAll()
    }

    @AfterEach
    fun tearDownConsumer() {
        kafkaConsumer.close()
    }

    @Test
    fun `publish sends outbox events to Kafka topic`() {
        val payload =
            """
            {"paymentId":"${UUID.randomUUID()}","userId":100001,"planId":"openai-basic"}
            """.trimIndent()
        val offsetBefore = currentTopicEndOffset()
        outboxRepository.save(
            OutboxPaymentEvent(
                eventType = TOPIC,
                payload = payload,
                createdAt = LocalDateTime.now(),
            ),
        )
        assertEquals(
            1,
            outboxRepository.findUnpublished(100).size,
            "seeded outbox event must be present",
        )

        outboxPublisher.publish()

        assertTrue(
            hasNewTopicRecords(offsetBefore, Duration.ofSeconds(10)),
            "Kafka topic offset did not increase after publish()",
        )
        assertEquals(
            0,
            outboxRepository.findUnpublished(100).size,
            "event must be marked as published",
        )
    }

    @Test
    fun `publish marks outbox events as published after sending`() {
        outboxRepository.save(
            OutboxPaymentEvent(
                eventType = TOPIC,
                payload =
                    """
                    {"paymentId":"${UUID.randomUUID()}","userId":100002,"planId":"openai-pro"}
                    """.trimIndent(),
                createdAt = LocalDateTime.now(),
            ),
        )
        val countBefore = outboxRepository.findUnpublished(100).size

        outboxPublisher.publish()

        val countAfter = outboxRepository.findUnpublished(100).size
        assertEquals(
            1,
            countBefore,
            "test precondition: one unpublished event must exist",
        )
        assertEquals(
            0,
            countAfter,
            "published event must be removed from unpublished queue",
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

    private fun waitForPartitionReady() {
        val deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos()
        while (System.nanoTime() < deadline) {
            kafkaConsumer.poll(Duration.ofMillis(200))
            val position =
                runCatching { kafkaConsumer.position(TOPIC_PARTITION) }
                    .getOrDefault(-1L)
            if (position >= 0L) {
                return
            }
        }
        error("Kafka consumer did not initialize position for topic partition: $TOPIC_PARTITION")
    }

    private fun ensureTopicExists() {
        AdminClient
            .create(
                mapOf(
                    AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                ),
            ).use { adminClient ->
                try {
                    adminClient
                        .createTopics(listOf(NewTopic(TOPIC, 1, 1)))
                        .all()
                        .get(10, TimeUnit.SECONDS)
                } catch (e: ExecutionException) {
                    if (e.cause !is TopicExistsException) {
                        throw e
                    }
                }
            }
    }

    private fun hasNewTopicRecords(
        initialOffset: Long,
        timeout: Duration,
    ): Boolean {
        val deadline = System.nanoTime() + timeout.toNanos()
        while (System.nanoTime() < deadline) {
            kafkaConsumer.poll(Duration.ofMillis(200))
            if (currentTopicEndOffset() > initialOffset) {
                return true
            }
        }
        return false
    }

    private fun currentTopicEndOffset(): Long =
        kafkaConsumer
            .endOffsets(listOf(TOPIC_PARTITION))[TOPIC_PARTITION]
            ?: error("Kafka did not return end offset for $TOPIC_PARTITION")
}
