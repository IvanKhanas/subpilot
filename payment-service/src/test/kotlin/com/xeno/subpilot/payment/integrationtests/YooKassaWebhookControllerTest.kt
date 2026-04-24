package com.xeno.subpilot.payment.integrationtests

import com.xeno.subpilot.payment.controller.YooKassaPaymentWebhookController
import com.xeno.subpilot.payment.dto.kafka.YooKassaWebhookEvent
import com.xeno.subpilot.payment.dto.kafka.YooKassaWebhookPayment
import com.xeno.subpilot.payment.service.YooKassaPaymentService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import java.util.UUID

@ExtendWith(MockKExtension::class)
class YooKassaWebhookControllerTest {

    @MockK(relaxed = true)
    lateinit var paymentService: YooKassaPaymentService

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
    }

    @BeforeEach
    fun setUp() {
        controller = YooKassaPaymentWebhookController(paymentService)
    }

    @Test
    fun `handleWebhook does not throw for payment succeeded event`() {
        controller.handleWebhook(succeededEvent)
    }

    @Test
    fun `handleWebhook does not throw for payment canceled event`() {
        controller.handleWebhook(canceledEvent)
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
}
