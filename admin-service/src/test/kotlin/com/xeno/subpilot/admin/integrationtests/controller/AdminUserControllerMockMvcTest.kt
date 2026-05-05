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
package com.xeno.subpilot.admin.integrationtests.controller

import com.xeno.subpilot.admin.controller.AdminUserController
import com.xeno.subpilot.admin.dto.AddSubscriptionRequest
import com.xeno.subpilot.admin.dto.AdjustLoyaltyRequest
import com.xeno.subpilot.admin.dto.UserInfoResponse
import com.xeno.subpilot.admin.integrationtests.MockMvcTestSupport
import com.xeno.subpilot.admin.service.AdminUserService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class AdminUserControllerMockMvcTest {

    enum class UserActionCase(
        val pathSuffix: String,
    ) {
        BAN("ban"),
        UNBAN("unban"),
    }

    @MockK
    lateinit var adminUserService: AdminUserService

    private lateinit var mockMvc: MockMvc

    private val faker = Faker()

    private lateinit var operator: String
    private var userId: Long = 0L
    private lateinit var planId: String
    private lateinit var idempotencyKey: String
    private lateinit var loyaltyReason: String

    @BeforeEach
    fun setUp() {
        operator = "admin-${faker.number().digits(6)}"
        userId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        planId = faker.regexify("[a-z]{8}") + "-pro"
        idempotencyKey = faker.internet().uuid()
        loyaltyReason = faker.lorem().sentence(4)

        mockMvc =
            MockMvcTestSupport.buildMockMvc(
                operator,
                AdminUserController(adminUserService),
            )

        coEvery { adminUserService.banUser(operator, userId) } returns Unit
        coEvery { adminUserService.unbanUser(operator, userId) } returns Unit
        coEvery { adminUserService.addSubscription(operator, userId, any()) } returns Unit
        coEvery { adminUserService.adjustLoyalty(operator, userId, any()) } returns Unit
    }

    @Test
    fun `GET user returns service response as JSON`() {
        val registeredAt = "2026-04-26 12:00"
        val role = "USER"
        val response =
            UserInfoResponse(
                userId = userId,
                blocked = false,
                role = role,
                registeredAt = registeredAt,
                loyaltyPoints = faker.number().numberBetween(10L, 1000L),
                freeRequestsRemaining = faker.number().numberBetween(1L, 100L),
                paidRequestsRemaining = faker.number().numberBetween(10L, 500L),
            )
        coEvery { adminUserService.getUserInfo(userId) } returns response

        MockMvcTestSupport
            .performSuspend(mockMvc, get("/admin/users/$userId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.role").value(role))
            .andExpect(jsonPath("$.registeredAt").value(registeredAt))

        coVerify(exactly = 1) { adminUserService.getUserInfo(userId) }
    }

    @ParameterizedTest(name = "POST /admin/users/id/{0} delegates with operator from JWT")
    @EnumSource(UserActionCase::class)
    fun `POST user action endpoints return 200 and delegate`(actionCase: UserActionCase) {
        MockMvcTestSupport
            .performSuspend(mockMvc, post("/admin/users/$userId/${actionCase.pathSuffix}"))
            .andExpect(status().isOk)

        when (actionCase) {
            UserActionCase.BAN ->
                coVerify(
                    exactly = 1,
                ) { adminUserService.banUser(operator, userId) }
            UserActionCase.UNBAN ->
                coVerify(
                    exactly = 1,
                ) { adminUserService.unbanUser(operator, userId) }
        }
    }

    @Test
    fun `POST add subscription returns 200 and maps JSON body`() {
        val body = """{"planId":"$planId","idempotencyKey":"$idempotencyKey"}"""
        val requestSlot = slot<AddSubscriptionRequest>()

        MockMvcTestSupport
            .performSuspend(
                mockMvc,
                post("/admin/users/$userId/subscription")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isOk)

        coVerify(
            exactly = 1,
        ) { adminUserService.addSubscription(operator, userId, capture(requestSlot)) }
        assertEquals(planId, requestSlot.captured.planId)
        assertEquals(idempotencyKey, requestSlot.captured.idempotencyKey)
    }

    @Test
    fun `POST add subscription returns 400 on invalid body`() {
        val body = """{"planId":"","idempotencyKey":"$idempotencyKey"}"""

        mockMvc
            .perform(
                post("/admin/users/$userId/subscription")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isBadRequest)

        coVerify(exactly = 0) { adminUserService.addSubscription(any(), any(), any()) }
    }

    @Test
    fun `POST adjust loyalty returns 200 and maps JSON body`() {
        val delta = faker.number().numberBetween(1L, 500L)
        val body =
            """
            {"delta":$delta,"reason":"$loyaltyReason","idempotencyKey":"$idempotencyKey"}
            """.trimIndent()
        val requestSlot = slot<AdjustLoyaltyRequest>()

        MockMvcTestSupport
            .performSuspend(
                mockMvc,
                post("/admin/users/$userId/loyalty/adjust")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isOk)

        coVerify(
            exactly = 1,
        ) { adminUserService.adjustLoyalty(operator, userId, capture(requestSlot)) }
        assertEquals(delta, requestSlot.captured.delta)
        assertEquals(loyaltyReason, requestSlot.captured.reason)
        assertEquals(idempotencyKey, requestSlot.captured.idempotencyKey)
    }

    @Test
    fun `POST adjust loyalty returns 400 on invalid body`() {
        val body = """{"delta":10,"reason":"","idempotencyKey":"$idempotencyKey"}"""

        mockMvc
            .perform(
                post("/admin/users/$userId/loyalty/adjust")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isBadRequest)

        coVerify(exactly = 0) { adminUserService.adjustLoyalty(any(), any(), any()) }
    }
}
