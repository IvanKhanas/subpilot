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
package com.xeno.subpilot.subscription.service.kafka

import com.xeno.subpilot.subscription.dto.kafka.PaymentSucceededEvent
import com.xeno.subpilot.subscription.dto.kafka.SubscriptionActivatedEvent
import com.xeno.subpilot.subscription.repository.PlanRepository
import com.xeno.subpilot.subscription.service.SubscriptionActivationService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class PaymentSucceededConsumer(
    private val activationService: SubscriptionActivationService,
    private val planRepository: PlanRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {

    @KafkaListener(topics = ["payment_succeeded"])
    fun consume(message: String) {
        val event = objectMapper.readValue(message, PaymentSucceededEvent::class.java)
        val activated = activationService.activate(event)
        if (activated) {
            publishActivated(event)
        }
    }

    private fun publishActivated(event: PaymentSucceededEvent) {
        val plan = planRepository.findById(event.planId) ?: return
        val notification =
            SubscriptionActivatedEvent(
                userId = event.userId,
                planDisplayName = plan.displayName,
                allocations =
                    plan.allocations.map {
                        SubscriptionActivatedEvent.ProviderAllocation(it.provider, it.requests)
                    },
            )
        kafkaTemplate.send("subscription_activated", objectMapper.writeValueAsString(notification))
    }
}
