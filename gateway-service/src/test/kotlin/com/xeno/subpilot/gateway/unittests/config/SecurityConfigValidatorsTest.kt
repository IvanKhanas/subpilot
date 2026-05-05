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

import com.xeno.subpilot.gateway.config.AudienceValidator
import com.xeno.subpilot.gateway.config.TokenTypeValidator
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt

class SecurityConfigValidatorsTest {

    @Test
    fun `AudienceValidator succeeds when required audience is present`() {
        val jwt = jwt(audience = listOf("subpilot-api", "other"))
        val result = AudienceValidator("subpilot-api").validate(jwt)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `AudienceValidator fails when required audience is absent`() {
        val jwt = jwt(audience = listOf("other"))
        val result = AudienceValidator("subpilot-api").validate(jwt)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `TokenTypeValidator succeeds when token_type matches`() {
        val jwt = jwt(tokenType = "access")
        val result = TokenTypeValidator("access").validate(jwt)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `TokenTypeValidator fails when token_type does not match`() {
        val jwt = jwt(tokenType = "refresh")
        val result = TokenTypeValidator("access").validate(jwt)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `TokenTypeValidator fails when token_type claim is absent`() {
        val jwt = jwt(tokenType = null)
        val result = TokenTypeValidator("access").validate(jwt)
        assertTrue(result.errors.isNotEmpty())
    }

    private fun jwt(
        audience: List<String> = listOf("subpilot-api"),
        tokenType: String? = "access",
    ): Jwt {
        val builder =
            Jwt
                .withTokenValue("token")
                .header("alg", "none")
                .subject("test")
                .audience(audience)
        if (tokenType != null) {
            builder.claim("token_type", tokenType)
        }
        return builder.build()
    }
}
