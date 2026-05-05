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
package com.xeno.subpilot.payment.integrationtests

import com.xeno.subpilot.payment.controller.YooKassaPaymentWebhookController
import com.xeno.subpilot.payment.dto.kafka.YooKassaWebhookEvent
import com.xeno.subpilot.payment.metrics.PaymentMetrics
import com.xeno.subpilot.payment.service.YooKassaPaymentService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ExtendWith(MockKExtension::class)
class YooKassaWebhookControllerMockMvcTest {

    @MockK
    lateinit var paymentService: YooKassaPaymentService

    private lateinit var mockMvc: MockMvc

    private lateinit var metrics: PaymentMetrics

    companion object {
        const val WEBHOOK_PATH = "/payment/webhook"
        const val YOOKASSA_PAYMENT_ID = "11111111-1111-1111-1111-111111111111"

        val webhookBody =
            """
            {
              "event": "payment.succeeded",
              "object": {
                "id": "$YOOKASSA_PAYMENT_ID",
                "status": "succeeded"
              }
            }
            """.trimIndent()
    }

    @BeforeEach
    fun setUp() {
        metrics =
            PaymentMetrics(
                io.micrometer.core.instrument.simple
                    .SimpleMeterRegistry(),
            )
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(YooKassaPaymentWebhookController(paymentService, metrics))
                .build()
    }

    @Test
    fun `POST webhook returns 200 and delegates event to payment service`() {
        val eventSlot = slot<YooKassaWebhookEvent>()
        every { paymentService.handlePaymentWebhook(capture(eventSlot)) } returns Unit

        mockMvc
            .perform(
                post(WEBHOOK_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(webhookBody),
            ).andExpect(status().isOk)

        verify(exactly = 1) { paymentService.handlePaymentWebhook(any()) }
        assertEquals("payment.succeeded", eventSlot.captured.event)
        assertEquals(
            YOOKASSA_PAYMENT_ID,
            eventSlot.captured.payment.id
                .toString(),
        )
    }

    @Test
    fun `POST webhook increments failure metric when service throws`() {
        every { paymentService.handlePaymentWebhook(any()) } throws RuntimeException("db down")
        val failuresBefore = metrics.webhookFailures.count()

        assertFailsWith<jakarta.servlet.ServletException> {
            mockMvc.perform(
                post(WEBHOOK_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(webhookBody),
            )
        }

        val failuresAfter = metrics.webhookFailures.count()
        assertEquals(failuresBefore + 1.0, failuresAfter)
    }
}
