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
package com.xeno.subpilot.admin.unittests.service

import com.xeno.subpilot.admin.client.SubscriptionAdminClient
import com.xeno.subpilot.admin.dto.AllocationRequest
import com.xeno.subpilot.admin.dto.CreatePlanRequest
import com.xeno.subpilot.admin.entity.AdminAction
import com.xeno.subpilot.admin.entity.AdminTargetType
import com.xeno.subpilot.admin.service.AdminSubscriptionService
import com.xeno.subpilot.admin.service.AuditService
import com.xeno.subpilot.admin.unittests.AdminTestFixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tools.jackson.databind.ObjectMapper

import java.math.BigDecimal

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class AdminSubscriptionServiceTest {

    @MockK
    lateinit var subscriptionClient: SubscriptionAdminClient

    @MockK
    lateinit var auditService: AuditService

    @MockK
    lateinit var objectMapper: ObjectMapper

    private lateinit var service: AdminSubscriptionService

    companion object {
        const val PLAN_ID = "combo-pro"
        const val DISPLAY_NAME = "Combo PRO"
        val PRICE: BigDecimal = BigDecimal("499.00")
        const val SERIALIZED_REQUEST = """{"kind":"create_plan"}"""
        const val OPENAI_PROVIDER = "openai"
        const val ANTHROPIC_PROVIDER = "anthropic"
        const val OPENAI_REQUESTS = 120
        const val ANTHROPIC_REQUESTS = 80
    }

    @BeforeEach
    fun setUp() {
        service =
            AdminSubscriptionService(
                subscriptionClient = subscriptionClient,
                auditService = auditService,
                objectMapper = objectMapper,
            )
    }

    @Test
    fun `createPlan maps request to grpc and records plan audit`() =
        runTest {
            val request =
                CreatePlanRequest(
                    planId = PLAN_ID,
                    provider = AdminTestFixtures.PROVIDER,
                    displayName = DISPLAY_NAME,
                    price = PRICE,
                    currency = AdminTestFixtures.CURRENCY,
                    allocations =
                        listOf(
                            AllocationRequest(OPENAI_PROVIDER, OPENAI_REQUESTS),
                            AllocationRequest(ANTHROPIC_PROVIDER, ANTHROPIC_REQUESTS),
                        ),
                )
            val expectedAllocations =
                listOf(
                    OPENAI_PROVIDER to OPENAI_REQUESTS,
                    ANTHROPIC_PROVIDER to ANTHROPIC_REQUESTS,
                )

            coEvery {
                subscriptionClient.createPlan(
                    planId = PLAN_ID,
                    provider = AdminTestFixtures.PROVIDER,
                    displayName = DISPLAY_NAME,
                    price = PRICE.toPlainString(),
                    currency = AdminTestFixtures.CURRENCY,
                    allocations = expectedAllocations,
                )
            } returns Unit
            every { objectMapper.writeValueAsString(request) } returns SERIALIZED_REQUEST
            justRun {
                auditService.record(
                    operator = AdminTestFixtures.OPERATOR,
                    action = AdminAction.CREATE_PLAN,
                    targetType = AdminTargetType.PLAN,
                    targetId = PLAN_ID,
                    payload = SERIALIZED_REQUEST,
                )
            }

            service.createPlan(AdminTestFixtures.OPERATOR, request)

            coVerify(exactly = 1) {
                subscriptionClient.createPlan(
                    planId = PLAN_ID,
                    provider = AdminTestFixtures.PROVIDER,
                    displayName = DISPLAY_NAME,
                    price = PRICE.toPlainString(),
                    currency = AdminTestFixtures.CURRENCY,
                    allocations = expectedAllocations,
                )
            }
            verify(exactly = 1) {
                auditService.record(
                    operator = AdminTestFixtures.OPERATOR,
                    action = AdminAction.CREATE_PLAN,
                    targetType = AdminTargetType.PLAN,
                    targetId = PLAN_ID,
                    payload = SERIALIZED_REQUEST,
                )
            }
        }
}
