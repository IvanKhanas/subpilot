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
package com.xeno.subpilot.admin.unittests.client

import com.xeno.subpilot.admin.client.GrpcRetry
import com.xeno.subpilot.admin.client.SubscriptionAdminGrpcClient
import com.xeno.subpilot.admin.config.GrpcRetryProperties
import com.xeno.subpilot.admin.exception.UserNotFoundException
import com.xeno.subpilot.admin.unittests.AdminTestFixtures
import com.xeno.subpilot.proto.subscription.v1.ActivateSubscriptionResponse
import com.xeno.subpilot.proto.subscription.v1.BlockUserResponse
import com.xeno.subpilot.proto.subscription.v1.CreatePlanResponse
import com.xeno.subpilot.proto.subscription.v1.GetBalanceResponse
import com.xeno.subpilot.proto.subscription.v1.GetUserInfoResponse
import com.xeno.subpilot.proto.subscription.v1.PlanAllocation
import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import com.xeno.subpilot.proto.subscription.v1.UnblockUserResponse
import io.grpc.Status
import io.grpc.StatusException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class SubscriptionAdminGrpcClientTest {

    @MockK
    lateinit var stub: SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineStub

    private lateinit var client: SubscriptionAdminGrpcClient

    companion object {
        const val DISPLAY_NAME = "OpenAI PRO"
        const val PRICE = "499.00"
        const val REQUESTS = 250
        const val ERROR_DESCRIPTION = "grpc failure"
    }

    private val grpcRetry =
        GrpcRetry(
            GrpcRetryProperties(maxAttempts = 1, initialBackoffMs = 0, backoffMultiplier = 1.0),
        )

    @BeforeEach
    fun setUp() {
        client = SubscriptionAdminGrpcClient(stub, grpcRetry)
    }

    @Test
    fun `getUserInfo returns grpc response when user exists`() =
        runTest {
            val response =
                GetUserInfoResponse
                    .newBuilder()
                    .setFound(true)
                    .setBlocked(false)
                    .setRole("USER")
                    .build()
            coEvery { stub.getUserInfo(any(), any()) } returns response

            val result = client.getUserInfo(AdminTestFixtures.USER_ID)

            assertSame(response, result)
            coVerify(exactly = 1) {
                stub.getUserInfo(match { it.userId == AdminTestFixtures.USER_ID }, any())
            }
        }

    @Test
    fun `getUserInfo throws UserNotFoundException when grpc reports not found`() =
        runTest {
            coEvery { stub.getUserInfo(any(), any()) } returns
                GetUserInfoResponse
                    .newBuilder()
                    .setFound(false)
                    .build()

            val error =
                assertFailsWith<UserNotFoundException> {
                    client.getUserInfo(
                        AdminTestFixtures.USER_ID,
                    )
                }

            assertEquals("User ${AdminTestFixtures.USER_ID} not found", error.message)
        }

    @Test
    fun `getBalance delegates request with user id`() =
        runTest {
            val response = GetBalanceResponse.getDefaultInstance()
            coEvery { stub.getBalance(any(), any()) } returns response

            val result = client.getBalance(AdminTestFixtures.USER_ID)

            assertSame(response, result)
            coVerify(exactly = 1) {
                stub.getBalance(match { it.userId == AdminTestFixtures.USER_ID }, any())
            }
        }

    @ParameterizedTest(name = "blocked={0}")
    @CsvSource(
        "true",
        "false",
    )
    fun `block and unblock forward request to grpc`(blocked: Boolean) =
        runTest {
            if (blocked) {
                coEvery { stub.blockUser(any(), any()) } returns
                    BlockUserResponse.getDefaultInstance()
            } else {
                coEvery { stub.unblockUser(any(), any()) } returns
                    UnblockUserResponse.getDefaultInstance()
            }

            if (blocked) {
                client.blockUser(AdminTestFixtures.USER_ID)
            } else {
                client.unblockUser(AdminTestFixtures.USER_ID)
            }

            if (blocked) {
                coVerify(exactly = 1) {
                    stub.blockUser(match { it.userId == AdminTestFixtures.USER_ID }, any())
                }
            } else {
                coVerify(exactly = 1) {
                    stub.unblockUser(match { it.userId == AdminTestFixtures.USER_ID }, any())
                }
            }
        }

    @Test
    fun `blockUser rethrows grpc StatusException`() =
        runTest {
            val exception = statusException(Status.NOT_FOUND)
            coEvery { stub.blockUser(any(), any()) } throws exception

            val thrown =
                assertFailsWith<StatusException> { client.blockUser(AdminTestFixtures.USER_ID) }

            assertSame(exception, thrown)
        }

    @Test
    fun `activateSubscription forwards all request fields`() =
        runTest {
            coEvery { stub.activateSubscription(any(), any()) } returns
                ActivateSubscriptionResponse.getDefaultInstance()

            client.activateSubscription(
                AdminTestFixtures.USER_ID,
                AdminTestFixtures.PLAN_ID,
                AdminTestFixtures.IDEMPOTENCY_KEY,
            )

            coVerify(exactly = 1) {
                stub.activateSubscription(
                    match {
                        it.userId == AdminTestFixtures.USER_ID &&
                            it.planId == AdminTestFixtures.PLAN_ID &&
                            it.idempotencyKey == AdminTestFixtures.IDEMPOTENCY_KEY
                    },
                    any(),
                )
            }
        }

    @Test
    fun `createPlan maps price and allocations to grpc request`() =
        runTest {
            val allocations = listOf(AdminTestFixtures.PROVIDER to REQUESTS)
            coEvery { stub.createPlan(any(), any()) } returns
                CreatePlanResponse.getDefaultInstance()

            client.createPlan(
                planId = AdminTestFixtures.PLAN_ID,
                provider = AdminTestFixtures.PROVIDER,
                displayName = DISPLAY_NAME,
                price = PRICE,
                currency = AdminTestFixtures.CURRENCY,
                allocations = allocations,
            )

            coVerify(exactly = 1) {
                stub.createPlan(
                    match {
                        it.planId == AdminTestFixtures.PLAN_ID &&
                            it.provider == AdminTestFixtures.PROVIDER &&
                            it.displayName == DISPLAY_NAME &&
                            it.price == PRICE &&
                            it.currency == AdminTestFixtures.CURRENCY &&
                            it.allocationsList ==
                            listOf(planAllocation(AdminTestFixtures.PROVIDER, REQUESTS))
                    },
                    any(),
                )
            }
        }

    @Test
    fun `createPlan rethrows grpc StatusException`() =
        runTest {
            val exception = statusException(Status.INVALID_ARGUMENT)
            coEvery { stub.createPlan(any(), any()) } throws exception

            val thrown =
                assertFailsWith<StatusException> {
                    client.createPlan(
                        planId = AdminTestFixtures.PLAN_ID,
                        provider = AdminTestFixtures.PROVIDER,
                        displayName = DISPLAY_NAME,
                        price = PRICE,
                        currency = AdminTestFixtures.CURRENCY,
                        allocations = listOf(AdminTestFixtures.PROVIDER to REQUESTS),
                    )
                }

            assertSame(exception, thrown)
        }

    private fun planAllocation(
        provider: String,
        requests: Int,
    ): PlanAllocation =
        PlanAllocation
            .newBuilder()
            .setProvider(provider)
            .setRequests(requests)
            .build()

    private fun statusException(status: Status): StatusException =
        status
            .withDescription(ERROR_DESCRIPTION)
            .asException()
}
