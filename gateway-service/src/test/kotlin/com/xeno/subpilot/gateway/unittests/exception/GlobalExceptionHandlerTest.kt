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
package com.xeno.subpilot.gateway.unittests.exception

import com.xeno.subpilot.gateway.exception.GatewayException
import com.xeno.subpilot.gateway.exception.GlobalExceptionHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `handleGateway returns the exception status and message`() {
        val ex = GatewayException(HttpStatus.NOT_FOUND, "resource_not_found")
        val response = handler.handleGateway(ex)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(mapOf("message" to "resource_not_found"), response.body)
    }

    @Test
    fun `handleGateway returns 401 for unauthorized exception`() {
        val ex = GatewayException(HttpStatus.UNAUTHORIZED, "invalid_credentials")
        val response = handler.handleGateway(ex)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `handleUnexpected returns 500`() {
        val response = handler.handleUnexpected(RuntimeException("unexpected"))

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals(mapOf("message" to "gateway_unexpected_error"), response.body)
    }
}
