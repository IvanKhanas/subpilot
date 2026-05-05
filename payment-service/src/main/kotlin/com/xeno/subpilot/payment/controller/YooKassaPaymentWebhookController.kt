/*
 * Copyright 2026 Ivan Khanas
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
package com.xeno.subpilot.payment.controller

import com.xeno.subpilot.payment.dto.kafka.YooKassaWebhookEvent
import com.xeno.subpilot.payment.metrics.PaymentMetrics
import com.xeno.subpilot.payment.service.YooKassaPaymentService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class YooKassaPaymentWebhookController(
    private val yooKassaPaymentService: YooKassaPaymentService,
    private val metrics: PaymentMetrics,
) {

    @PostMapping("/payment/webhook")
    fun handleWebhook(
        @RequestBody event: YooKassaWebhookEvent,
    ) {
        try {
            yooKassaPaymentService.handlePaymentWebhook(event)
        } catch (ex: Exception) {
            metrics.webhookFailures.increment()
            throw ex
        }
    }
}
