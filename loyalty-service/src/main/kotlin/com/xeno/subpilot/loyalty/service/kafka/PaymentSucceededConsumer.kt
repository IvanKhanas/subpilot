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
package com.xeno.subpilot.loyalty.service.kafka

import com.xeno.subpilot.loyalty.dto.kafka.PaymentSucceededEvent
import com.xeno.subpilot.loyalty.service.LoyaltyService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

private val logger = KotlinLogging.logger {}

@Component
class PaymentSucceededConsumer(
    private val loyaltyService: LoyaltyService,
    private val objectMapper: ObjectMapper,
) {

    @KafkaListener(topics = ["payment_succeeded"])
    fun consume(message: String) {
        val event = objectMapper.readValue(message, PaymentSucceededEvent::class.java)
        logger.atDebug {
            this.message = "loyalty_payment_succeeded_received"
            payload = mapOf("payment_id" to event.paymentId, "user_id" to event.userId)
        }
        loyaltyService.earn(event)
    }
}
