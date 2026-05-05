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
package com.xeno.subpilot.admin.auth

import com.xeno.subpilot.admin.config.AdminAuthProperties
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class AuthJwtTokenFactory(
    private val objectMapper: ObjectMapper,
    properties: AdminAuthProperties,
) {
    private val secret = properties.jwt.secret
    private val issuer = properties.jwt.issuer
    private val audience = properties.jwt.audience
    private val accessTtlSeconds = properties.jwt.accessTtlSeconds
    private val refreshTtlSeconds = properties.jwt.refreshTtlSeconds

    fun issueTokens(
        subject: String,
        scope: String,
    ): TokenPair {
        val accessToken = issueAccessToken(subject, scope)
        val refreshToken = issueRefreshToken(subject)
        return TokenPair(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = accessTtlSeconds,
        )
    }

    private fun issueAccessToken(
        subject: String,
        scope: String,
    ): String =
        issueToken(
            subject = subject,
            tokenType = "access",
            ttlSeconds = accessTtlSeconds,
            extraClaims = mapOf("scope" to scope),
        )

    private fun issueRefreshToken(subject: String): String =
        issueToken(
            subject = subject,
            tokenType = "refresh",
            ttlSeconds = refreshTtlSeconds,
            extraClaims = emptyMap(),
        )

    private fun issueToken(
        subject: String,
        tokenType: String,
        ttlSeconds: Long,
        extraClaims: Map<String, Any>,
    ): String {
        val now = Instant.now().epochSecond
        val header = mapOf("alg" to "HS256", "typ" to "JWT")
        val payload =
            mutableMapOf<String, Any>(
                "sub" to subject,
                "iss" to issuer,
                "aud" to listOf(audience),
                "token_type" to tokenType,
                "iat" to now,
                "exp" to (now + ttlSeconds),
            ).apply { putAll(extraClaims) }

        val encodedHeader = encode(objectMapper.writeValueAsBytes(header))
        val encodedPayload = encode(objectMapper.writeValueAsBytes(payload))
        val signingInput = "$encodedHeader.$encodedPayload"
        val signature = hmacSha256(signingInput)
        return "$signingInput.${encode(signature)}"
    }

    private fun hmacSha256(signingInput: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)
        return mac.doFinal(signingInput.toByteArray(StandardCharsets.UTF_8))
    }

    private fun encode(input: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(input)
}

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)
