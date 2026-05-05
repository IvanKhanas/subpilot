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
package com.xeno.subpilot.admin.unittests.controller

import com.xeno.subpilot.admin.controller.AdminUserController
import com.xeno.subpilot.admin.dto.AddSubscriptionRequest
import com.xeno.subpilot.admin.dto.AdjustLoyaltyRequest
import com.xeno.subpilot.admin.dto.UserInfoResponse
import com.xeno.subpilot.admin.service.AdminUserService
import com.xeno.subpilot.admin.unittests.AdminTestFixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt

import kotlin.test.assertEquals
import kotlin.test.assertSame

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class AdminUserControllerTest {

    @MockK
    lateinit var adminUserService: AdminUserService

    @MockK
    lateinit var jwt: Jwt

    private lateinit var controller: AdminUserController

    companion object {
        const val ADJUST_REASON = "manual grant"
        const val ADJUST_DELTA = 50L
    }

    @BeforeEach
    fun setUp() {
        controller = AdminUserController(adminUserService)
        every { jwt.subject } returns AdminTestFixtures.OPERATOR
    }

    @Test
    fun `getUser returns service response`() =
        runTest {
            val response =
                UserInfoResponse(
                    userId = AdminTestFixtures.USER_ID,
                    blocked = false,
                    role = "USER",
                    registeredAt = "2024-01-01 00:00",
                    loyaltyPoints = 50L,
                    freeRequestsRemaining = 10L,
                    paidRequestsRemaining = 20L,
                )
            coEvery { adminUserService.getUserInfo(AdminTestFixtures.USER_ID) } returns response

            val result = controller.getUser(AdminTestFixtures.USER_ID)

            assertSame(response, result)
            coVerify(exactly = 1) { adminUserService.getUserInfo(AdminTestFixtures.USER_ID) }
        }

    @Test
    fun `banUser delegates with operator subject and returns OK`() =
        runTest {
            coEvery {
                adminUserService.banUser(AdminTestFixtures.OPERATOR, AdminTestFixtures.USER_ID)
            } returns Unit

            val response = controller.banUser(AdminTestFixtures.USER_ID, jwt)

            assertEquals(HttpStatus.OK, response.statusCode)
            coVerify(exactly = 1) {
                adminUserService.banUser(AdminTestFixtures.OPERATOR, AdminTestFixtures.USER_ID)
            }
        }

    @Test
    fun `unbanUser delegates with operator subject and returns OK`() =
        runTest {
            coEvery {
                adminUserService.unbanUser(AdminTestFixtures.OPERATOR, AdminTestFixtures.USER_ID)
            } returns Unit

            val response = controller.unbanUser(AdminTestFixtures.USER_ID, jwt)

            assertEquals(HttpStatus.OK, response.statusCode)
            coVerify(exactly = 1) {
                adminUserService.unbanUser(AdminTestFixtures.OPERATOR, AdminTestFixtures.USER_ID)
            }
        }

    @Test
    fun `addSubscription delegates request and returns OK`() =
        runTest {
            val request =
                AddSubscriptionRequest(
                    planId = AdminTestFixtures.PLAN_ID,
                    idempotencyKey = AdminTestFixtures.IDEMPOTENCY_KEY,
                )
            coEvery {
                adminUserService.addSubscription(
                    AdminTestFixtures.OPERATOR,
                    AdminTestFixtures.USER_ID,
                    request,
                )
            } returns Unit

            val response = controller.addSubscription(AdminTestFixtures.USER_ID, request, jwt)

            assertEquals(HttpStatus.OK, response.statusCode)
            coVerify(exactly = 1) {
                adminUserService.addSubscription(
                    AdminTestFixtures.OPERATOR,
                    AdminTestFixtures.USER_ID,
                    request,
                )
            }
        }

    @Test
    fun `adjustLoyalty delegates request and returns OK`() =
        runTest {
            val request =
                AdjustLoyaltyRequest(
                    delta = ADJUST_DELTA,
                    reason = ADJUST_REASON,
                    idempotencyKey = AdminTestFixtures.IDEMPOTENCY_KEY,
                )
            coEvery {
                adminUserService.adjustLoyalty(
                    AdminTestFixtures.OPERATOR,
                    AdminTestFixtures.USER_ID,
                    request,
                )
            } returns Unit

            val response = controller.adjustLoyalty(AdminTestFixtures.USER_ID, request, jwt)

            assertEquals(HttpStatus.OK, response.statusCode)
            coVerify(exactly = 1) {
                adminUserService.adjustLoyalty(
                    AdminTestFixtures.OPERATOR,
                    AdminTestFixtures.USER_ID,
                    request,
                )
            }
        }
}
