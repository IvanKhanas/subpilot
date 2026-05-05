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

import com.xeno.subpilot.admin.client.LoyaltyAdminClient
import com.xeno.subpilot.admin.client.SubscriptionAdminClient
import com.xeno.subpilot.admin.dto.AddSubscriptionRequest
import com.xeno.subpilot.admin.dto.AdjustLoyaltyRequest
import com.xeno.subpilot.admin.entity.AdminAction
import com.xeno.subpilot.admin.entity.AdminTargetType
import com.xeno.subpilot.admin.service.AdminUserService
import com.xeno.subpilot.admin.service.AuditService
import com.xeno.subpilot.admin.unittests.AdminTestFixtures
import com.xeno.subpilot.proto.subscription.v1.GetBalanceResponse
import com.xeno.subpilot.proto.subscription.v1.GetUserInfoResponse
import com.xeno.subpilot.proto.subscription.v1.ProviderBalance
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import tools.jackson.databind.ObjectMapper

import java.util.stream.Stream

import kotlin.test.assertEquals

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class AdminUserServiceTest {

    @MockK
    lateinit var subscriptionClient: SubscriptionAdminClient

    @MockK
    lateinit var loyaltyClient: LoyaltyAdminClient

    @MockK
    lateinit var auditService: AuditService

    @MockK
    lateinit var objectMapper: ObjectMapper

    private lateinit var service: AdminUserService

    companion object {
        const val ROLE = "USER"
        const val REGISTERED_AT_EPOCH = 1_704_067_200L
        const val EXPECTED_REGISTERED_AT = "2024-01-01 00:00"
        const val LOYALTY_POINTS = 125L
        const val SERIALIZED_REQUEST = """{"payload":"value"}"""
        const val ADJUST_REASON = "manual grant"
        const val DELTA = 50L
        const val BAN_OPERATION = "ban"
        const val UNBAN_OPERATION = "unban"

        @JvmStatic
        fun userStateCases(): Stream<Arguments> =
            Stream.of(
                arguments(BAN_OPERATION, AdminAction.BAN_USER),
                arguments(UNBAN_OPERATION, AdminAction.UNBAN_USER),
            )
    }

    @BeforeEach
    fun setUp() {
        service =
            AdminUserService(
                subscriptionClient = subscriptionClient,
                loyaltyClient = loyaltyClient,
                auditService = auditService,
                objectMapper = objectMapper,
            )
    }

    @Test
    fun `getUserInfo aggregates balances and formats registeredAt in UTC`() =
        runTest {
            coEvery { subscriptionClient.getUserInfo(AdminTestFixtures.USER_ID) } returns
                GetUserInfoResponse
                    .newBuilder()
                    .setFound(true)
                    .setBlocked(false)
                    .setRole(ROLE)
                    .setRegisteredAtEpoch(REGISTERED_AT_EPOCH)
                    .build()
            coEvery { subscriptionClient.getBalance(AdminTestFixtures.USER_ID) } returns
                GetBalanceResponse
                    .newBuilder()
                    .addFreeBalances(providerBalance("openai", 10))
                    .addFreeBalances(providerBalance("anthropic", 20))
                    .addPaidBalances(providerBalance("openai", 30))
                    .build()
            coEvery { loyaltyClient.getBalance(AdminTestFixtures.USER_ID) } returns LOYALTY_POINTS

            val response = service.getUserInfo(AdminTestFixtures.USER_ID)

            assertEquals(AdminTestFixtures.USER_ID, response.userId)
            assertEquals(false, response.blocked)
            assertEquals(ROLE, response.role)
            assertEquals(EXPECTED_REGISTERED_AT, response.registeredAt)
            assertEquals(LOYALTY_POINTS, response.loyaltyPoints)
            assertEquals(30L, response.freeRequestsRemaining)
            assertEquals(30L, response.paidRequestsRemaining)
        }

    @ParameterizedTest(name = "{0} writes {1} audit action")
    @MethodSource("userStateCases")
    fun `ban and unban delegate to grpc and write audit`(
        operation: String,
        expectedAction: AdminAction,
    ) = runTest {
        justRun {
            auditService.record(
                operator = AdminTestFixtures.OPERATOR,
                action = expectedAction,
                targetType = AdminTargetType.USER,
                targetId = AdminTestFixtures.USER_ID.toString(),
            )
        }
        when (operation) {
            BAN_OPERATION ->
                coEvery { subscriptionClient.blockUser(AdminTestFixtures.USER_ID) } returns
                    Unit
            UNBAN_OPERATION ->
                coEvery { subscriptionClient.unblockUser(AdminTestFixtures.USER_ID) } returns
                    Unit
        }

        when (operation) {
            BAN_OPERATION -> service.banUser(AdminTestFixtures.OPERATOR, AdminTestFixtures.USER_ID)
            UNBAN_OPERATION ->
                service.unbanUser(
                    AdminTestFixtures.OPERATOR,
                    AdminTestFixtures.USER_ID,
                )
        }

        when (operation) {
            BAN_OPERATION ->
                coVerify(
                    exactly = 1,
                ) { subscriptionClient.blockUser(AdminTestFixtures.USER_ID) }
            UNBAN_OPERATION ->
                coVerify(exactly = 1) {
                    subscriptionClient.unblockUser(AdminTestFixtures.USER_ID)
                }
        }
        verify(exactly = 1) {
            auditService.record(
                operator = AdminTestFixtures.OPERATOR,
                action = expectedAction,
                targetType = AdminTargetType.USER,
                targetId = AdminTestFixtures.USER_ID.toString(),
            )
        }
    }

    @Test
    fun `addSubscription calls grpc activate and writes audit payload`() =
        runTest {
            val request =
                AddSubscriptionRequest(
                    planId = AdminTestFixtures.PLAN_ID,
                    idempotencyKey = AdminTestFixtures.IDEMPOTENCY_KEY,
                )
            coEvery {
                subscriptionClient.activateSubscription(
                    AdminTestFixtures.USER_ID,
                    AdminTestFixtures.PLAN_ID,
                    AdminTestFixtures.IDEMPOTENCY_KEY,
                )
            } returns Unit
            every { objectMapper.writeValueAsString(request) } returns SERIALIZED_REQUEST
            justRun {
                auditService.record(
                    operator = AdminTestFixtures.OPERATOR,
                    action = AdminAction.ADD_SUBSCRIPTION,
                    targetType = AdminTargetType.USER,
                    targetId = AdminTestFixtures.USER_ID.toString(),
                    payload = SERIALIZED_REQUEST,
                )
            }

            service.addSubscription(AdminTestFixtures.OPERATOR, AdminTestFixtures.USER_ID, request)

            coVerify(exactly = 1) {
                subscriptionClient.activateSubscription(
                    AdminTestFixtures.USER_ID,
                    AdminTestFixtures.PLAN_ID,
                    AdminTestFixtures.IDEMPOTENCY_KEY,
                )
            }
            verify(exactly = 1) {
                auditService.record(
                    operator = AdminTestFixtures.OPERATOR,
                    action = AdminAction.ADD_SUBSCRIPTION,
                    targetType = AdminTargetType.USER,
                    targetId = AdminTestFixtures.USER_ID.toString(),
                    payload = SERIALIZED_REQUEST,
                )
            }
        }

    @Test
    fun `adjustLoyalty calls grpc adjust and writes audit payload`() =
        runTest {
            val request =
                AdjustLoyaltyRequest(
                    delta = DELTA,
                    reason = ADJUST_REASON,
                    idempotencyKey = AdminTestFixtures.IDEMPOTENCY_KEY,
                )
            coEvery {
                loyaltyClient.adjustPoints(
                    AdminTestFixtures.USER_ID,
                    DELTA,
                    ADJUST_REASON,
                    AdminTestFixtures.IDEMPOTENCY_KEY,
                )
            } returns Unit
            every { objectMapper.writeValueAsString(request) } returns SERIALIZED_REQUEST
            justRun {
                auditService.record(
                    operator = AdminTestFixtures.OPERATOR,
                    action = AdminAction.ADJUST_LOYALTY,
                    targetType = AdminTargetType.USER,
                    targetId = AdminTestFixtures.USER_ID.toString(),
                    payload = SERIALIZED_REQUEST,
                )
            }

            service.adjustLoyalty(AdminTestFixtures.OPERATOR, AdminTestFixtures.USER_ID, request)

            coVerify(exactly = 1) {
                loyaltyClient.adjustPoints(
                    AdminTestFixtures.USER_ID,
                    DELTA,
                    ADJUST_REASON,
                    AdminTestFixtures.IDEMPOTENCY_KEY,
                )
            }
            verify(exactly = 1) {
                auditService.record(
                    operator = AdminTestFixtures.OPERATOR,
                    action = AdminAction.ADJUST_LOYALTY,
                    targetType = AdminTargetType.USER,
                    targetId = AdminTestFixtures.USER_ID.toString(),
                    payload = SERIALIZED_REQUEST,
                )
            }
        }

    private fun providerBalance(
        provider: String,
        requestsRemaining: Int,
    ): ProviderBalance =
        ProviderBalance
            .newBuilder()
            .setProvider(provider)
            .setRequestsRemaining(requestsRemaining)
            .build()
}
