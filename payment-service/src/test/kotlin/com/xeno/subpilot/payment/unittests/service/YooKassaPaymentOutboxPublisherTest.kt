package com.xeno.subpilot.payment.unittests.service

import com.xeno.subpilot.payment.entity.OutboxPaymentEvent
import com.xeno.subpilot.payment.properties.YooKassaPaymentOutboxProperties
import com.xeno.subpilot.payment.repository.OutboxPaymentEventJpaRepository
import com.xeno.subpilot.payment.service.kafka.YooKassaPaymentOutboxPublisher
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.kafka.core.KafkaTemplate

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.CompletableFuture

import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class YooKassaPaymentOutboxPublisherTest {

    @MockK lateinit var outboxRepository: OutboxPaymentEventJpaRepository

    @MockK lateinit var kafkaTemplate: KafkaTemplate<String, String>

    private val fixedClock: Clock =
        Clock.fixed(
            Instant.parse("2025-01-15T10:00:00Z"),
            ZoneOffset.UTC,
        )
    private val properties =
        YooKassaPaymentOutboxProperties(
            schedulerInterval = Duration.ofSeconds(5),
            batchSize = BATCH_SIZE,
        )

    private lateinit var publisher: YooKassaPaymentOutboxPublisher

    companion object {
        const val TOPIC = "payment_succeeded"
        const val BATCH_SIZE = 10
        const val PAYLOAD_1 = """{"payment_id":"aaa","user_id":1}"""
        const val PAYLOAD_2 = """{"payment_id":"bbb","user_id":2}"""
    }

    @BeforeEach
    fun setUp() {
        publisher =
            YooKassaPaymentOutboxPublisher(
                outboxPaymentEventJpaRepository = outboxRepository,
                yooKassaPaymentOutboxProperties = properties,
                kafkaTemplate = kafkaTemplate,
                clock = fixedClock,
            )
    }

    @Test
    fun `publish does nothing when no unpublished events`() {
        every { outboxRepository.findUnpublished(BATCH_SIZE) } returns emptyList()

        publisher.publish()

        verify(exactly = 0) { kafkaTemplate.send(any<String>(), any<String>()) }
        verify(exactly = 0) { outboxRepository.markPublished(any(), any()) }
    }

    @Test
    fun `publish sends each event payload to Kafka topic`() {
        val events = listOf(outboxEvent(1L, PAYLOAD_1), outboxEvent(2L, PAYLOAD_2))
        every { outboxRepository.findUnpublished(BATCH_SIZE) } returns events
        every { kafkaTemplate.send(any<String>(), any<String>()) } returns
            CompletableFuture.completedFuture(null)
        every { outboxRepository.markPublished(any(), any()) } returns Unit

        publisher.publish()

        verify { kafkaTemplate.send(TOPIC, PAYLOAD_1) }
        verify { kafkaTemplate.send(TOPIC, PAYLOAD_2) }
    }

    @Test
    fun `publish marks all sent events as published`() {
        val events = listOf(outboxEvent(1L, PAYLOAD_1), outboxEvent(2L, PAYLOAD_2))
        every { outboxRepository.findUnpublished(BATCH_SIZE) } returns events
        every { kafkaTemplate.send(any<String>(), any<String>()) } returns
            CompletableFuture.completedFuture(null)
        val markedIds = slot<List<Long>>()
        every { outboxRepository.markPublished(capture(markedIds), any()) } returns Unit

        publisher.publish()

        assertEquals(listOf(1L, 2L), markedIds.captured)
    }

    @Test
    fun `publish uses batch size from properties`() {
        every { outboxRepository.findUnpublished(BATCH_SIZE) } returns emptyList()

        publisher.publish()

        verify { outboxRepository.findUnpublished(BATCH_SIZE) }
    }

    private fun outboxEvent(
        id: Long,
        payload: String,
    ): OutboxPaymentEvent =
        OutboxPaymentEvent(
            id = id,
            eventType = "payment_succeeded",
            payload = payload,
            createdAt = LocalDateTime.now(fixedClock),
        )
}
