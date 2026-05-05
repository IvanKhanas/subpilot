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
package com.xeno.subpilot.gateway.unittests.auth

import com.xeno.subpilot.gateway.auth.InternalJwtTokenFactory
import com.xeno.subpilot.gateway.config.GatewayProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

import java.util.Base64

class InternalJwtTokenFactoryTest {

    @Test
    fun `issues internal jwt preserving subject and scope`() {
        val properties =
            GatewayProperties(
                auth =
                    GatewayProperties.AuthProperties(
                        jwt =
                            GatewayProperties.AuthJwtProperties(
                                secret = "auth-secret",
                                issuer = "subpilot-admin",
                                audience = "subpilot-api",
                                accessTtlSeconds = 900,
                                refreshTtlSeconds = 1209600,
                            ),
                    ),
                internalJwt =
                    GatewayProperties.InternalJwtProperties(
                        secret = "internal-secret",
                        ttlSeconds = 60,
                    ),
                routes =
                    listOf(
                        GatewayProperties.RouteProperties(
                            id = "admin-service",
                            pathPrefix = "/api/v1/admin",
                            targetBaseUrl = "http://admin-service:8086",
                        ),
                    ),
            )

        val token =
            InternalJwtTokenFactory(
                ObjectMapper(),
                properties,
            ).issue(subject = "ivan", scope = "admin.read")
        val parts = token.split('.')

        assertEquals(3, parts.size)

        val payloadJson = String(Base64.getUrlDecoder().decode(pad(parts[1])))
        assertTrue(payloadJson.contains("\"sub\":\"ivan\""))
        assertTrue(payloadJson.contains("\"scope\":\"admin.read\""))
    }

    private fun pad(value: String): String {
        val remainder = value.length % 4
        return if (remainder == 0) value else value + "=".repeat(4 - remainder)
    }
}
