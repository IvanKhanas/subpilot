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
package com.xeno.subpilot.admin.unittests.auth

import com.xeno.subpilot.admin.auth.AuthJwtTokenFactory
import com.xeno.subpilot.admin.config.AdminAuthProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

import java.util.Base64

class AuthJwtTokenFactoryTest {

    @Test
    fun `issues access and refresh tokens`() {
        val tokenPair =
            AuthJwtTokenFactory(
                ObjectMapper(),
                properties(),
            ).issueTokens("ivan", "admin.read")

        assertEquals(900, tokenPair.expiresIn)

        val accessPayload = decodePayload(tokenPair.accessToken)
        val refreshPayload = decodePayload(tokenPair.refreshToken)

        assertTrue(accessPayload.contains("\"token_type\":\"access\""))
        assertTrue(accessPayload.contains("\"scope\":\"admin.read\""))
        assertTrue(refreshPayload.contains("\"token_type\":\"refresh\""))
    }

    private fun decodePayload(token: String): String {
        val payloadPart = token.split('.')[1]
        return String(Base64.getUrlDecoder().decode(pad(payloadPart)))
    }

    private fun pad(value: String): String {
        val remainder = value.length % 4
        return if (remainder == 0) value else value + "=".repeat(4 - remainder)
    }

    private fun properties() =
        AdminAuthProperties(
            jwt =
                AdminAuthProperties.JwtProperties(
                    secret = "auth-secret",
                    issuer = "subpilot-admin",
                    audience = "subpilot-api",
                    accessTtlSeconds = 900,
                    refreshTtlSeconds = 1209600,
                ),
            bootstrap =
                AdminAuthProperties.BootstrapProperties(
                    username = "",
                    password = "",
                    scopes = "admin.read admin.write",
                ),
        )
}
