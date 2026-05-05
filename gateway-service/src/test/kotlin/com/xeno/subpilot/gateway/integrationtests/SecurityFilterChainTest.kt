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
package com.xeno.subpilot.gateway.integrationtests

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import jakarta.servlet.Filter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.client.RestTemplate
import org.springframework.web.context.WebApplicationContext

import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = [
        "gateway.auth.jwt.secret=a-secret-that-is-long-enough-for-hs256-algorithm",
        "gateway.auth.jwt.issuer=subpilot-admin",
        "gateway.auth.jwt.audience=subpilot-api",
        "gateway.internal-jwt.secret=internal-secret-that-is-long-enough-for-hmac",
    ],
)
class SecurityFilterChainTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    @MockkBean
    private lateinit var restTemplate: RestTemplate

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val securityFilter = context.getBean("springSecurityFilterChain") as Filter
        val builder = MockMvcBuilders.webAppContextSetup(context)
        builder.addFilter<DefaultMockMvcBuilder>(securityFilter, "/*")
        mockMvc = builder.build()
    }

    @Test
    fun `actuator health is accessible without authentication`() {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk)
    }

    @Test
    fun `POST auth login is accessible without authentication`() {
        every {
            restTemplate.exchange(any<URI>(), HttpMethod.POST, any(), ByteArray::class.java)
        } returns ResponseEntity.ok(ByteArray(0))

        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andExpect(status().isOk)
    }

    @Test
    fun `POST auth refresh is accessible without authentication`() {
        every {
            restTemplate.exchange(any<URI>(), HttpMethod.POST, any(), ByteArray::class.java)
        } returns ResponseEntity.ok(ByteArray(0))

        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andExpect(status().isOk)
    }

    @Test
    fun `admin endpoint returns 401 without JWT`() {
        mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `admin GET endpoint returns 200 with read scope JWT`() {
        every {
            restTemplate.exchange(any<URI>(), HttpMethod.GET, any(), ByteArray::class.java)
        } returns ResponseEntity.ok(ByteArray(0))

        mockMvc
            .perform(
                get("/api/v1/admin/users")
                    .header("Authorization", "Bearer ${buildAccessToken("ivan", "admin.read")}"),
            ).andExpect(status().isOk)
    }

    @Test
    fun `admin DELETE endpoint returns 403 with read-only scope JWT`() {
        mockMvc
            .perform(
                delete("/api/v1/admin/users/1")
                    .header("Authorization", "Bearer ${buildAccessToken("ivan", "admin.read")}"),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `authenticated request to unknown path returns 403`() {
        mockMvc
            .perform(
                get("/unknown-path")
                    .header("Authorization", "Bearer ${buildAccessToken("ivan", "admin.read")}"),
            ).andExpect(status().isForbidden)
    }

    private fun buildAccessToken(
        subject: String,
        scope: String,
    ): String {
        val secret = "a-secret-that-is-long-enough-for-hs256-algorithm"
        val now = Instant.now().epochSecond
        val header = """{"alg":"HS256","typ":"JWT"}"""
        val payload =
            """{"sub":"$subject","iss":"subpilot-admin","aud":["subpilot-api"],"token_type":"access","scope":"$scope","iat":$now,"exp":${now + 900}}"""

        val encodedHeader = encode(header.toByteArray())
        val encodedPayload = encode(payload.toByteArray())
        val signingInput = "$encodedHeader.$encodedPayload"
        val signature = hmacSha256(signingInput, secret)
        return "$signingInput.${encode(signature)}"
    }

    private fun hmacSha256(
        input: String,
        secret: String,
    ): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(input.toByteArray(StandardCharsets.UTF_8))
    }

    private fun encode(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
