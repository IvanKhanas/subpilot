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
