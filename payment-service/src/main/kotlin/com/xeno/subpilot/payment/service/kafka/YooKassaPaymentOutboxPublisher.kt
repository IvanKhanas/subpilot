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
