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
package com.xeno.subpilot.tgbot.unittests.client

import com.xeno.subpilot.proto.subscription.v1.BlockUserResponse
import com.xeno.subpilot.proto.subscription.v1.GetBalanceResponse
import com.xeno.subpilot.proto.subscription.v1.GetPlanInfoResponse
import com.xeno.subpilot.proto.subscription.v1.GetPlansResponse
import com.xeno.subpilot.proto.subscription.v1.GetUserInfoResponse
import com.xeno.subpilot.proto.subscription.v1.PlanAllocation
import com.xeno.subpilot.proto.subscription.v1.PlanInfo
import com.xeno.subpilot.proto.subscription.v1.ProviderBalance
import com.xeno.subpilot.proto.subscription.v1.RegisterUserResponse
import com.xeno.subpilot.proto.subscription.v1.SetModelPreferenceResponse
import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import com.xeno.subpilot.proto.subscription.v1.UnblockUserResponse
import com.xeno.subpilot.tgbot.client.GrpcRetry
import com.xeno.subpilot.tgbot.client.SubscriptionGrpcClient
import com.xeno.subpilot.tgbot.config.GrpcRetryProperties
import com.xeno.subpilot.tgbot.exception.SubscriptionServiceException
import io.grpc.Status
import io.grpc.StatusException
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

import kotlinx.coroutines.runBlocking

@ExtendWith(MockKExtension::class)
class SubscriptionGrpcClientTest {

    @MockK(relaxed = true)
    lateinit var stub: SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineStub

    private val faker = Faker()

    private lateinit var client: SubscriptionGrpcClient
    private var userId: Long = 0L
    private val modelId = "gpt-4o"
    private val planId = "openai-basic"

    @BeforeEach
    fun setUp() {
        userId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        client =
            SubscriptionGrpcClient(
                stub,
                GrpcRetry(
                    GrpcRetryProperties(
                        maxAttempts = 1,
                        initialBackoffMs = 0,
                        backoffMultiplier = 1.0,
                    ),
                ),
            )
    }

    @Test
    fun `registerUser returns RegistrationResult for new user`() {
        coEvery { stub.registerUser(any(), any()) } returns
            RegisterUserResponse
                .newBuilder()
                .setIsNew(true)
                .setFreeQuota(10)
                .setFreeModelId("gpt-4o-mini")
                .build()

        val result = runBlocking { client.registerUser(userId) }

        assertTrue(result!!.isNew)
        assertEquals(10, result.freeQuota)
    }

    @Test
    fun `registerUser returns null on StatusException`() {
        coEvery { stub.registerUser(any(), any()) } throws StatusException(Status.UNAVAILABLE)

        val result = runBlocking { client.registerUser(userId) }

        assertNull(result)
    }

    @Test
    fun `setModelPreference returns providerChanged from stub`() {
        coEvery { stub.setModelPreference(any(), any()) } returns
            SetModelPreferenceResponse
                .newBuilder()
                .setProviderChanged(
                    true,
                ).setModelCost(10)
                .setProvider("openai")
                .build()

        val result = runBlocking { client.setModelPreference(userId, modelId) }

        assertTrue(result.providerChanged)
    }

    @Test
    fun `registerUser throws when subscription returns unknown free model`() {
        coEvery { stub.registerUser(any(), any()) } returns
            RegisterUserResponse
                .newBuilder()
                .setIsNew(true)
                .setFreeQuota(10)
                .setFreeModelId("unknown-model")
                .build()

        assertThrows<IllegalStateException> {
            runBlocking { client.registerUser(userId) }
        }
    }

    @Test
    fun `getPlans maps plans and allocations from gRPC response`() {
        val plan =
            PlanInfo
                .newBuilder()
                .setPlanId("openai-basic")
                .setProvider("openai")
                .setDisplayName("Basic")
                .setPrice("199.00")
                .setCurrency("RUB")
                .addAllocations(
                    PlanAllocation
                        .newBuilder()
                        .setProvider("openai")
                        .setRequests(100)
                        .build(),
                ).build()
        coEvery { stub.getPlans(any(), any()) } returns
            GetPlansResponse.newBuilder().addPlans(plan).build()

        val result = runBlocking { client.getPlans() }

        assertEquals(1, result.size)
        assertEquals("openai-basic", result[0].planId)
        assertEquals("openai", result[0].provider)
        assertEquals("199.00", result[0].price)
        assertEquals(1, result[0].allocations.size)
        assertEquals(100, result[0].allocations[0].requests)
    }

    @Test
    fun `getPlanInfo returns mapped dto when plan exists`() {
        val plan =
            PlanInfo
                .newBuilder()
                .setPlanId("openai-basic")
                .setProvider("openai")
                .setDisplayName("Basic")
                .setPrice("199.00")
                .setCurrency("RUB")
                .addAllocations(
                    PlanAllocation
                        .newBuilder()
                        .setProvider("openai")
                        .setRequests(100)
                        .build(),
                ).build()
        coEvery { stub.getPlanInfo(any(), any()) } returns
            GetPlanInfoResponse.newBuilder().setPlan(plan).build()

        val result = runBlocking { client.getPlanInfo(planId) }

        assertEquals("openai-basic", result?.planId)
        assertEquals("Basic", result?.displayName)
        assertEquals(1, result?.allocations?.size)
    }

    @Test
    fun `getPlanInfo returns null on NOT_FOUND status`() {
        coEvery { stub.getPlanInfo(any(), any()) } throws StatusException(Status.NOT_FOUND)

        val result = runBlocking { client.getPlanInfo("missing-plan") }

        assertNull(result)
    }

    @Test
    fun `getPlanInfo throws SubscriptionServiceException on non-NOT_FOUND status`() {
        coEvery { stub.getPlanInfo(any(), any()) } throws StatusException(Status.UNAVAILABLE)

        assertThrows<SubscriptionServiceException> {
            runBlocking { client.getPlanInfo(planId) }
        }
    }

    @Test
    fun `getBalance maps free and paid provider balances`() {
        coEvery { stub.getBalance(any(), any()) } returns
            GetBalanceResponse
                .newBuilder()
                .addFreeBalances(
                    ProviderBalance
                        .newBuilder()
                        .setProvider("openai")
                        .setRequestsRemaining(7)
                        .setNextResetAtEpoch(1_777_000_000L)
                        .build(),
                ).addPaidBalances(
                    ProviderBalance
                        .newBuilder()
                        .setProvider("openai")
                        .setRequestsRemaining(120)
                        .build(),
                ).build()

        val result = runBlocking { client.getBalance(userId) }

        assertEquals(1, result.freeBalances.size)
        assertEquals("openai", result.freeBalances[0].provider)
        assertEquals(7, result.freeBalances[0].requestsRemaining)
        assertEquals(1_777_000_000L, result.freeBalances[0].nextResetAt.epochSecond)

        assertEquals(1, result.paidBalances.size)
        assertEquals("openai", result.paidBalances[0].provider)
        assertEquals(120, result.paidBalances[0].requestsRemaining)
    }

    @Test
    fun `getUserInfo maps response when user exists`() {
        coEvery { stub.getUserInfo(any(), any()) } returns
            GetUserInfoResponse
                .newBuilder()
                .setFound(true)
                .setBlocked(true)
                .setRole("ADMIN")
                .setRegisteredAtEpoch(1_700_000_000L)
                .build()

        val result = runBlocking { client.getUserInfo(userId) }

        assertEquals(true, result?.blocked)
        assertEquals("ADMIN", result?.role)
        assertEquals(1_700_000_000L, result?.registeredAtEpoch)
    }

    @Test
    fun `getUserInfo returns null when response found flag is false`() {
        coEvery { stub.getUserInfo(any(), any()) } returns
            GetUserInfoResponse
                .newBuilder()
                .setFound(false)
                .build()

        val result = runBlocking { client.getUserInfo(userId) }

        assertNull(result)
    }

    @Test
    fun `getUserInfo returns null on gRPC failure`() {
        coEvery { stub.getUserInfo(any(), any()) } throws StatusException(Status.UNAVAILABLE)

        val result = runBlocking { client.getUserInfo(userId) }

        assertNull(result)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ToggleOperation::class)
    fun `block and unblock complete when gRPC call succeeds`(operation: ToggleOperation) {
        when (operation) {
            ToggleOperation.BLOCK -> {
                coEvery { stub.blockUser(any(), any()) } returns
                    BlockUserResponse.getDefaultInstance()
            }
            ToggleOperation.UNBLOCK -> {
                coEvery { stub.unblockUser(any(), any()) } returns
                    UnblockUserResponse.getDefaultInstance()
            }
        }

        runBlocking {
            when (operation) {
                ToggleOperation.BLOCK -> client.blockUser(userId)
                ToggleOperation.UNBLOCK -> client.unblockUser(userId)
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(FailingOperation::class)
    fun `throws SubscriptionServiceException for failing gRPC calls`(operation: FailingOperation) {
        when (operation) {
            FailingOperation.SET_MODEL_PREFERENCE -> {
                coEvery { stub.setModelPreference(any(), any()) } throws
                    StatusException(Status.UNAVAILABLE)
            }
            FailingOperation.GET_PLANS -> {
                coEvery { stub.getPlans(any(), any()) } throws StatusException(Status.UNAVAILABLE)
            }
            FailingOperation.GET_BALANCE -> {
                coEvery { stub.getBalance(any(), any()) } throws StatusException(Status.UNAVAILABLE)
            }
            FailingOperation.BLOCK_USER -> {
                coEvery { stub.blockUser(any(), any()) } throws StatusException(Status.UNAVAILABLE)
            }
            FailingOperation.UNBLOCK_USER -> {
                coEvery { stub.unblockUser(any(), any()) } throws
                    StatusException(Status.UNAVAILABLE)
            }
        }

        assertThrows<SubscriptionServiceException> {
            runBlocking {
                when (operation) {
                    FailingOperation.SET_MODEL_PREFERENCE ->
                        client.setModelPreference(
                            userId,
                            modelId,
                        )
                    FailingOperation.GET_PLANS -> client.getPlans()
                    FailingOperation.GET_BALANCE -> client.getBalance(userId)
                    FailingOperation.BLOCK_USER -> client.blockUser(userId)
                    FailingOperation.UNBLOCK_USER -> client.unblockUser(userId)
                }
            }
        }
    }

    enum class ToggleOperation {
        BLOCK,
        UNBLOCK,
    }

    enum class FailingOperation {
        SET_MODEL_PREFERENCE,
        GET_PLANS,
        GET_BALANCE,
        BLOCK_USER,
        UNBLOCK_USER,
    }
}
