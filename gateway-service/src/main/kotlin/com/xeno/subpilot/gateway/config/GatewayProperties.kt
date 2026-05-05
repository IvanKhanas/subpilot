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
package com.xeno.subpilot.gateway.config

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "gateway")
@Validated
data class GatewayProperties(
    @field:Valid
    val auth: AuthProperties,

    @field:Valid
    val internalJwt: InternalJwtProperties,

    @field:NotEmpty
    @field:Valid
    val routes: List<RouteProperties>,
) {

    fun findRoute(path: String): RouteProperties? =
        routes
            .sortedByDescending { it.pathPrefix.length }
            .firstOrNull { path == it.pathPrefix || path.startsWith("${it.pathPrefix}/") }

    @Validated
    data class AuthProperties(
        @field:Valid
        val jwt: AuthJwtProperties,
    )

    @Validated
    data class AuthJwtProperties(
        @field:NotBlank
        val secret: String,

        @field:NotBlank
        val issuer: String,

        @field:NotBlank
        val audience: String,

        @field:Min(1)
        val accessTtlSeconds: Long,

        @field:Min(1)
        val refreshTtlSeconds: Long,
    )

    @Validated
    data class InternalJwtProperties(
        @field:NotBlank
        val secret: String,

        @field:Min(1)
        val ttlSeconds: Long,
    )

    @Validated
    data class RouteProperties(
        @field:NotBlank
        val id: String,

        @field:NotBlank
        val pathPrefix: String,

        @field:NotBlank
        val targetBaseUrl: String,
    )
}
