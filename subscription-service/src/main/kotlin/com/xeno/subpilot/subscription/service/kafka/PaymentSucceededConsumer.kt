package com.xeno.subpilot.subscription.service.kafka

import com.xeno.subpilot.subscription.dto.kafka.PaymentSucceededEvent
import com.xeno.subpilot.subscription.dto.kafka.SubscriptionActivatedEvent
import com.xeno.subpilot.subscription.properties.SubscriptionProperties
import com.xeno.subpilot.subscription.service.SubscriptionActivationService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class PaymentSucceededConsumer(
    private val activationService: SubscriptionActivationService,
    private val subscriptionProperties: SubscriptionProperties,
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
        val plan = subscriptionProperties.plans[event.planId] ?: return
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
