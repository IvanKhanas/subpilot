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
