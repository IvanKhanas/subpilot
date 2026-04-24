package com.xeno.subpilot.payment.controller

import com.xeno.subpilot.payment.dto.kafka.YooKassaWebhookEvent
import com.xeno.subpilot.payment.service.YooKassaPaymentService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class YooKassaPaymentWebhookController(
    private val yooKassaPaymentService: YooKassaPaymentService,
) {

    @PostMapping("/payment/webhook")
    fun handleWebhook(
        @RequestBody event: YooKassaWebhookEvent,
    ) {
        yooKassaPaymentService.handlePaymentWebhook(event)
    }
}
