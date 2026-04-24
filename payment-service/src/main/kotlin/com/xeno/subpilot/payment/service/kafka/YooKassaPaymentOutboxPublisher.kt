package com.xeno.subpilot.payment.service.kafka

import com.xeno.subpilot.payment.properties.YooKassaPaymentOutboxProperties
import com.xeno.subpilot.payment.repository.OutboxPaymentEventJpaRepository
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.Clock
import java.time.LocalDateTime

@Service
class YooKassaPaymentOutboxPublisher(
    private val outboxPaymentEventJpaRepository: OutboxPaymentEventJpaRepository,
    private val yooKassaPaymentOutboxProperties: YooKassaPaymentOutboxProperties,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val clock: Clock,
) {

    @Scheduled(fixedDelayString = "\${yookassa-outbox.scheduler-interval}")
    @Transactional
    fun publish() {
        val events =
            outboxPaymentEventJpaRepository
                .findUnpublished(yooKassaPaymentOutboxProperties.batchSize)
        if (events.isEmpty()) return
        events
            .map { event -> kafkaTemplate.send("payment_succeeded", event.payload) }
            .forEach { it.get() }

        outboxPaymentEventJpaRepository.markPublished(
            ids = events.mapNotNull { it.id },
            now = LocalDateTime.now(clock),
        )
    }
}
