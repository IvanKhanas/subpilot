package com.xeno.subpilot.tgbot.unittests.exception

import com.xeno.subpilot.tgbot.exception.LoyaltyServiceException
import com.xeno.subpilot.tgbot.exception.PaymentServiceException
import com.xeno.subpilot.tgbot.exception.SubscriptionServiceException
import org.junit.jupiter.api.Test

import kotlin.test.assertEquals
import kotlin.test.assertSame

class ServiceExceptionsTest {

    @Test
    fun `loyalty and payment exceptions preserve message and cause`() {
        val cause = RuntimeException("down")

        val loyalty = LoyaltyServiceException("loyalty failed", cause)
        val payment = PaymentServiceException("payment failed", cause)

        assertEquals("loyalty failed", loyalty.message)
        assertSame(cause, loyalty.cause)
        assertEquals("payment failed", payment.message)
        assertSame(cause, payment.cause)
    }

    @Test
    fun `subscription exception supports nullable cause`() {
        val exception = SubscriptionServiceException("subscription failed")

        assertEquals("subscription failed", exception.message)
        assertEquals(null, exception.cause)
    }
}
