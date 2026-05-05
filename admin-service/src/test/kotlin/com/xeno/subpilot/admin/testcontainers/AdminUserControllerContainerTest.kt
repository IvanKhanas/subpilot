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
package com.xeno.subpilot.admin.testcontainers

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.ninjasquad.springmockk.MockkBean
import com.xeno.subpilot.admin.client.LoyaltyAdminClient
import com.xeno.subpilot.admin.client.PaymentAdminClient
import com.xeno.subpilot.admin.client.SubscriptionAdminClient
import com.xeno.subpilot.admin.entity.AdminAction
import com.xeno.subpilot.admin.entity.AdminTargetType
import com.xeno.subpilot.admin.repository.AuditLogJpaRepository
import io.mockk.coJustRun
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

import java.util.Date
import java.util.UUID

import javax.crypto.spec.SecretKeySpec

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminUserControllerContainerTest {

    @MockkBean
    lateinit var subscriptionClient: SubscriptionAdminClient

    @MockkBean
    lateinit var loyaltyClient: LoyaltyAdminClient

    @MockkBean
    lateinit var paymentClient: PaymentAdminClient

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    lateinit var auditLogRepository: AuditLogJpaRepository

    private lateinit var restTemplate: RestTemplate

    companion object {
        private val faker = Faker()
        private val postgres = TestContainersConfiguration.postgres

        const val JWT_SECRET = "test-secret-must-be-at-least-32-chars!!"
        const val OPERATOR = "test-operator"

        @JvmStatic
        @DynamicPropertySource
        fun configure(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.security.oauth2.resourceserver.jwt.secret") { JWT_SECRET }
        }
    }

    @BeforeEach
    fun setUp() {
        restTemplate = RestTemplate()
    }

    @ParameterizedTest(name = "POST /{0} writes {1} audit log")
    @CsvSource(
        "ban, BAN_USER",
        "unban, UNBAN_USER",
    )
    fun `ban and unban write USER audit log entry`(
        path: String,
        expectedAction: AdminAction,
    ) {
        val userId = randomUserId()
        mockUserStateChange(path, userId)

        val response = post("/api/v1/admin/users/$userId/$path", writeToken())

        assertEquals(HttpStatus.OK, response.statusCode)
        val log =
            auditLogRepository
                .findAll()
                .single { it.action == expectedAction && it.targetId == userId.toString() }
        assertEquals(AdminTargetType.USER, log.targetType)
        assertEquals(OPERATOR, log.operator)
    }

    @Test
    fun `POST loyalty adjust writes ADJUST_LOYALTY audit log entry`() {
        val userId = randomUserId()
        val idempotencyKey = UUID.randomUUID()
        coJustRun { loyaltyClient.adjustPoints(any(), any(), any(), any()) }

        val body = """{"delta":50,"idempotency_key":"$idempotencyKey","reason":"test-grant"}"""
        val response = post("/api/v1/admin/users/$userId/loyalty/adjust", writeToken(), body)

        assertEquals(HttpStatus.OK, response.statusCode)
        val log =
            auditLogRepository
                .findAll()
                .single {
                    it.action == AdminAction.ADJUST_LOYALTY &&
                        it.targetId == userId.toString()
                }
        assertEquals(AdminTargetType.USER, log.targetType)
        assertEquals(OPERATOR, log.operator)
        assertTrue(log.payload?.contains("50") == true)
    }

    @Test
    fun `POST ban returns 401 without token`() {
        val error =
            assertFailsWith<HttpClientErrorException.Unauthorized> {
                restTemplate.exchange(
                    "http://localhost:$port/api/v1/admin/users/123/ban",
                    HttpMethod.POST,
                    HttpEntity<Unit>(HttpHeaders()),
                    String::class.java,
                )
            }

        assertEquals(HttpStatus.UNAUTHORIZED, error.statusCode)
    }

    @Test
    fun `POST ban returns 403 for read-only token`() {
        val userId = randomUserId()

        val error =
            assertFailsWith<HttpClientErrorException.Forbidden> {
                post("/api/v1/admin/users/$userId/ban", readToken())
            }

        assertEquals(HttpStatus.FORBIDDEN, error.statusCode)
    }

    private fun post(
        path: String,
        token: String,
        body: String? = null,
    ) = restTemplate.exchange(
        "http://localhost:$port$path",
        HttpMethod.POST,
        HttpEntity(body, bearerHeaders(token)),
        String::class.java,
    )

    private fun bearerHeaders(token: String) =
        HttpHeaders().apply {
            set(HttpHeaders.AUTHORIZATION, "Bearer $token")
            set(HttpHeaders.CONTENT_TYPE, "application/json")
        }

    private fun writeToken() = jwt("admin.read admin.write")

    private fun readToken() = jwt("admin.read")

    private fun jwt(scope: String): String {
        val key = SecretKeySpec(JWT_SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val now = System.currentTimeMillis()
        val claims =
            JWTClaimsSet
                .Builder()
                .subject(OPERATOR)
                .issueTime(Date(now))
                .expirationTime(Date(now + 3_600_000))
                .claim("scope", scope)
                .build()
        val signed = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims)
        signed.sign(MACSigner(key))
        return signed.serialize()
    }

    private fun mockUserStateChange(
        path: String,
        userId: Long,
    ) {
        when (path) {
            "ban" -> coJustRun { subscriptionClient.blockUser(userId) }
            "unban" -> coJustRun { subscriptionClient.unblockUser(userId) }
        }
    }

    private fun randomUserId(): Long = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
}
