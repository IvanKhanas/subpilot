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

import com.xeno.subpilot.gateway.config.GatewayProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GatewayPropertiesTest {

    @Test
    fun `findRoute selects the longest matching prefix`() {
        val properties =
            properties(
                routes =
                    listOf(
                        GatewayProperties.RouteProperties(
                            id = "api-v1",
                            pathPrefix = "/api/v1",
                            targetBaseUrl = "http://generic:8080",
                        ),
                        GatewayProperties.RouteProperties(
                            id = "admin-service",
                            pathPrefix = "/api/v1/admin",
                            targetBaseUrl = "http://admin-service:8086",
                        ),
                    ),
            )

        val route = properties.findRoute("/api/v1/admin/users/42")

        assertNotNull(route)
        assertEquals("admin-service", route?.id)
    }

    @Test
    fun `findRoute returns null when route is missing`() {
        val properties =
            properties(
                routes =
                    listOf(
                        GatewayProperties.RouteProperties(
                            id = "admin-service",
                            pathPrefix = "/api/v1/admin",
                            targetBaseUrl = "http://admin-service:8086",
                        ),
                    ),
            )

        assertNull(properties.findRoute("/health"))
    }

    private fun properties(routes: List<GatewayProperties.RouteProperties>) =
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
            routes = routes,
        )
}
