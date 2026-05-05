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
package com.xeno.subpilot.admin.unittests.controller

import com.xeno.subpilot.admin.controller.AdminSubscriptionController
import com.xeno.subpilot.admin.dto.AllocationRequest
import com.xeno.subpilot.admin.dto.CreatePlanRequest
import com.xeno.subpilot.admin.service.AdminSubscriptionService
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

import java.math.BigDecimal

import kotlin.test.assertEquals

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class AdminSubscriptionControllerTest {

    @MockK
    lateinit var adminSubscriptionService: AdminSubscriptionService

    @MockK
    lateinit var jwt: Jwt

    private lateinit var controller: AdminSubscriptionController

    companion object {
        const val PLAN_ID = "combo-pro"
        const val DISPLAY_NAME = "Combo PRO"
    }

    @BeforeEach
    fun setUp() {
        controller = AdminSubscriptionController(adminSubscriptionService)
        every { jwt.subject } returns AdminTestFixtures.OPERATOR
    }

    @Test
    fun `createPlan delegates with operator and returns CREATED`() =
        runTest {
            val request =
                CreatePlanRequest(
                    planId = PLAN_ID,
                    provider = AdminTestFixtures.PROVIDER,
                    displayName = DISPLAY_NAME,
                    price = BigDecimal("499.00"),
                    currency = AdminTestFixtures.CURRENCY,
                    allocations = listOf(AllocationRequest(AdminTestFixtures.PROVIDER, 150)),
                )
            coEvery {
                adminSubscriptionService.createPlan(
                    AdminTestFixtures.OPERATOR,
                    request,
                )
            } returns
                Unit

            val response = controller.createPlan(request, jwt)

            assertEquals(HttpStatus.CREATED, response.statusCode)
            coVerify(
                exactly = 1,
            ) { adminSubscriptionService.createPlan(AdminTestFixtures.OPERATOR, request) }
        }
}
