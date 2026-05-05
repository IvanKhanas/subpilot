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
package com.xeno.subpilot.payment.integrationtests

import com.xeno.subpilot.payment.controller.YooKassaPaymentWebhookController
import com.xeno.subpilot.payment.dto.kafka.YooKassaWebhookEvent
import com.xeno.subpilot.payment.dto.kafka.YooKassaWebhookPayment
import com.xeno.subpilot.payment.metrics.PaymentMetrics
import com.xeno.subpilot.payment.service.YooKassaPaymentService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import java.util.UUID
import java.util.stream.Stream

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class YooKassaWebhookControllerTest {

    @MockK(relaxed = true)
    lateinit var paymentService: YooKassaPaymentService

    private val meterRegistry = SimpleMeterRegistry()
    private val metrics = PaymentMetrics(meterRegistry)

    private lateinit var controller: YooKassaPaymentWebhookController

    companion object {
        const val YOOKASSA_PAYMENT_ID = "11111111-1111-1111-1111-111111111111"

        val succeededEvent =
            YooKassaWebhookEvent(
                event = "payment.succeeded",
                payment =
                    YooKassaWebhookPayment(
                        id = UUID.fromString(YOOKASSA_PAYMENT_ID),
                        status = "succeeded",
                    ),
            )

        val canceledEvent =
            YooKassaWebhookEvent(
                event = "payment.canceled",
                payment =
                    YooKassaWebhookPayment(
                        id = UUID.fromString(YOOKASSA_PAYMENT_ID),
                        status = "canceled",
                    ),
            )

        @JvmStatic
        fun supportedWebhookEvents(): Stream<Arguments> =
            Stream.of(
                Arguments.of("payment succeeded", succeededEvent),
                Arguments.of("payment canceled", canceledEvent),
            )
    }

    @BeforeEach
    fun setUp() {
        controller = YooKassaPaymentWebhookController(paymentService, metrics)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("supportedWebhookEvents")
    fun `handleWebhook does not throw for supported events`(
        caseName: String,
        event: YooKassaWebhookEvent,
    ) {
        assertTrue(caseName.isNotBlank())
        controller.handleWebhook(event)
    }

    @Test
    fun `handleWebhook delegates to payment service`() {
        controller.handleWebhook(succeededEvent)

        verify { paymentService.handlePaymentWebhook(succeededEvent) }
    }

    @Test
    fun `handleWebhook passes event fields unchanged to service`() {
        val eventSlot = slot<YooKassaWebhookEvent>()
        every { paymentService.handlePaymentWebhook(capture(eventSlot)) } returns Unit

        controller.handleWebhook(succeededEvent)

        assert(eventSlot.captured.event == "payment.succeeded")
        assert(
            eventSlot.captured.payment.id
                .toString() == YOOKASSA_PAYMENT_ID,
        )
    }

    @Test
    fun `handleWebhook increments webhook_failures_total and rethrows on service exception`() {
        every { paymentService.handlePaymentWebhook(any()) } throws RuntimeException("db error")

        assertFailsWith<RuntimeException> {
            controller.handleWebhook(succeededEvent)
        }

        assertEquals(1.0, meterRegistry.counter("webhook_failures_total").count())
    }
}
