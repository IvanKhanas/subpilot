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
package com.xeno.subpilot.loyalty.unittests.exception

import com.xeno.subpilot.loyalty.exception.LoyaltyException
import com.xeno.subpilot.loyalty.exception.LoyaltyGrpcExceptionHandler
import io.grpc.Status
import org.junit.jupiter.api.Test

import kotlin.test.assertEquals
import kotlin.test.assertSame

class LoyaltyGrpcExceptionHandlerTest {

    private val handler = LoyaltyGrpcExceptionHandler()

    @Test
    fun `handleException maps LoyaltyException to its grpc status`() {
        val loyaltyException = LoyaltyException(Status.NOT_FOUND, "balance not found")

        val statusException = handler.handleException(loyaltyException)

        assertEquals(Status.NOT_FOUND.code, statusException.status.code)
        assertEquals("balance not found", statusException.status.description)
        assertSame(loyaltyException, statusException.cause)
    }

    @Test
    fun `handleException maps unexpected exception to internal grpc status`() {
        val exception = IllegalStateException("unexpected failure")

        val statusException = handler.handleException(exception)

        assertEquals(Status.INTERNAL.code, statusException.status.code)
        assertEquals("unexpected failure", statusException.status.description)
        assertSame(exception, statusException.cause)
    }
}
