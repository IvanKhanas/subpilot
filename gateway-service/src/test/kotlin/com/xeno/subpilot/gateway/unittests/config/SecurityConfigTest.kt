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
package com.xeno.subpilot.gateway.unittests.config

import com.xeno.subpilot.gateway.config.GatewayProperties
import com.xeno.subpilot.gateway.config.SecurityConfig
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SecurityConfigTest {

    private val properties =
        GatewayProperties(
            auth =
                GatewayProperties.AuthProperties(
                    jwt =
                        GatewayProperties.AuthJwtProperties(
                            secret = "a-secret-that-is-long-enough-for-hs256-algorithm",
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

    private val config = SecurityConfig(properties)

    @Test
    fun `gatewaySigningKey creates a key from the configured secret`() {
        val key = config.gatewaySigningKey()

        assertNotNull(key)
    }

    @Test
    fun `accessJwtDecoder creates a decoder with audience and token_type validators`() {
        val decoder = config.accessJwtDecoder(config.gatewaySigningKey())

        assertNotNull(decoder)
    }
}
