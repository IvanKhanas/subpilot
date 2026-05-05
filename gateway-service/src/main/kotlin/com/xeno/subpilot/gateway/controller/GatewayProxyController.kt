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
package com.xeno.subpilot.gateway.controller

import com.xeno.subpilot.gateway.auth.InternalJwtTokenFactory
import com.xeno.subpilot.gateway.config.GatewayProperties
import com.xeno.subpilot.gateway.exception.GatewayException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@RestController
class GatewayProxyController(
    private val gatewayProperties: GatewayProperties,
    private val tokenFactory: InternalJwtTokenFactory,
    private val restTemplate: RestTemplate,
) {

    @RequestMapping(
        path = ["/api/v1/**"],
        method = [
            RequestMethod.GET,
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.PATCH,
            RequestMethod.DELETE,
        ],
    )
    fun proxy(
        request: HttpServletRequest,
        @AuthenticationPrincipal jwt: Jwt?,
        @RequestBody(required = false) body: ByteArray?,
    ): ResponseEntity<ByteArray> {
        val route =
            gatewayProperties.findRoute(request.requestURI)
                ?: throw GatewayException(HttpStatus.NOT_FOUND, "gateway_route_not_found")

        val payload = body ?: ByteArray(0)
        val targetUri =
            UriComponentsBuilder
                .fromUriString(route.targetBaseUrl)
                .path(request.requestURI)
                .query(request.queryString)
                .build(true)
                .toUri()

        val requestHeaders = copyRequestHeaders(request)
        requestHeaders.set("X-Gateway-Route-Id", route.id)

        if (jwt != null) {
            val subject =
                jwt.subject?.takeIf { it.isNotBlank() }
                    ?: throw GatewayException(HttpStatus.UNAUTHORIZED, "gateway_subject_missing")
            val scope = extractScope(jwt)
            requestHeaders.setBearerAuth(tokenFactory.issue(subject, scope))
        }

        val upstreamResponse: ResponseEntity<ByteArray> =
            restTemplate.exchange(
                targetUri,
                HttpMethod.valueOf(request.method),
                HttpEntity(payload, requestHeaders),
                ByteArray::class.java,
            )

        return ResponseEntity
            .status(upstreamResponse.statusCode)
            .headers(filterResponseHeaders(upstreamResponse.headers))
            .body(upstreamResponse.body ?: ByteArray(0))
    }

    private fun extractScope(jwt: Jwt): String {
        val fromScopeClaim = jwt.getClaimAsString("scope")?.trim()
        if (!fromScopeClaim.isNullOrBlank()) {
            return fromScopeClaim
        }

        val fromScpClaim =
            jwt
                .getClaimAsStringList("scp")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.joinToString(" ")

        if (!fromScpClaim.isNullOrBlank()) {
            return fromScpClaim
        }

        throw GatewayException(HttpStatus.UNAUTHORIZED, "gateway_scope_missing")
    }

    private fun copyRequestHeaders(request: HttpServletRequest): HttpHeaders {
        val headers = HttpHeaders()
        val headerNames = request.headerNames

        while (headerNames.hasMoreElements()) {
            val name = headerNames.nextElement()
            if (name.lowercase() in REQUEST_HEADER_BLOCKLIST) {
                continue
            }

            val values = request.getHeaders(name)
            while (values.hasMoreElements()) {
                headers.add(name, values.nextElement())
            }
        }

        return headers
    }

    private fun filterResponseHeaders(source: HttpHeaders): HttpHeaders {
        val filtered = HttpHeaders()
        source.forEach { key, value ->
            if (key.lowercase() !in RESPONSE_HEADER_BLOCKLIST) {
                filtered.put(key, value.toMutableList())
            }
        }
        return filtered
    }

    companion object {
        private val REQUEST_HEADER_BLOCKLIST =
            setOf(
                HttpHeaders.HOST.lowercase(),
                HttpHeaders.CONTENT_LENGTH.lowercase(),
                HttpHeaders.AUTHORIZATION.lowercase(),
            )

        private val RESPONSE_HEADER_BLOCKLIST =
            setOf(
                HttpHeaders.TRANSFER_ENCODING.lowercase(),
                HttpHeaders.CONTENT_LENGTH.lowercase(),
                HttpHeaders.CONNECTION.lowercase(),
                HttpHeaders.TE.lowercase(),
                "keep-alive",
                "proxy-authenticate",
                "proxy-authorization",
                "trailer",
                "upgrade",
            )
    }
}
