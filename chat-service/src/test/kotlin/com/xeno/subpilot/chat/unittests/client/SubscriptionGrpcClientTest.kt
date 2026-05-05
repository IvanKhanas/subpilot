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
package com.xeno.subpilot.chat.unittests.client

import com.xeno.subpilot.chat.client.GrpcRetry
import com.xeno.subpilot.chat.client.SubscriptionGrpcClient
import com.xeno.subpilot.chat.config.GrpcRetryProperties
import com.xeno.subpilot.chat.exception.SubscriptionServiceException
import com.xeno.subpilot.proto.subscription.v1.CheckAccessResponse
import com.xeno.subpilot.proto.subscription.v1.GetModelPreferenceResponse
import com.xeno.subpilot.proto.subscription.v1.RefundAccessResponse
import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import io.grpc.Status
import io.grpc.StatusException
import io.mockk.coEvery
import io.mockk.coVerify
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
import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class SubscriptionGrpcClientTest {

    @MockK(relaxed = true)
    lateinit var stub: SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineStub

    private val faker = Faker()

    private lateinit var client: SubscriptionGrpcClient
    private var userId: Long = 0L

    @BeforeEach
    fun setUp() {
        userId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        client =
            SubscriptionGrpcClient(
                stub,
                GrpcRetry(
                    GrpcRetryProperties(
                        maxAttempts = 1,
                        initialBackoffMs = 1,
                        backoffMultiplier = 1.0,
                    ),
                ),
            )
    }

    @Test
    fun `checkAccess returns response from stub`() =
        runTest {
            val response = CheckAccessResponse.newBuilder().setAllowed(true).build()
            coEvery { stub.checkAccess(any(), any()) } returns response

            val result = client.checkAccess(userId = userId, modelId = "gpt-4o")

            assertTrue(result.allowed)
        }

    @ParameterizedTest(name = "{0}")
    @EnumSource(FailingCall::class)
    fun `throws SubscriptionServiceException for failing gRPC calls`(failingCall: FailingCall) =
        runTest {
            when (failingCall) {
                FailingCall.CHECK_ACCESS -> {
                    coEvery { stub.checkAccess(any(), any()) } throws
                        StatusException(Status.UNAVAILABLE)
                }
                FailingCall.GET_MODEL_PREFERENCE -> {
                    coEvery { stub.getModelPreference(any(), any()) } throws
                        StatusException(Status.UNAVAILABLE)
                }
            }

            assertThrows<SubscriptionServiceException> {
                when (failingCall) {
                    FailingCall.CHECK_ACCESS ->
                        client.checkAccess(
                            userId = userId,
                            modelId = "gpt-4o",
                        )
                    FailingCall.GET_MODEL_PREFERENCE -> client.getModelPreference(userId = userId)
                }
            }
        }

    @Test
    fun `getModelPreference returns modelId from stub`() =
        runTest {
            coEvery { stub.getModelPreference(any(), any()) } returns
                GetModelPreferenceResponse.newBuilder().setModelId("gpt-4o-mini").build()

            val result = client.getModelPreference(userId = userId)

            assertEquals("gpt-4o-mini", result)
        }

    @Test
    fun `refundAccess delegates to stub`() =
        runTest {
            coEvery { stub.refundAccess(any(), any()) } returns
                RefundAccessResponse.getDefaultInstance()

            client.refundAccess(
                userId = userId,
                modelId = "gpt-4o",
                freeConsumed = 1,
                paidConsumed = 2,
            )

            coVerify { stub.refundAccess(any(), any()) }
        }

    @Test
    fun `refundAccess swallows StatusException`() =
        runTest {
            coEvery { stub.refundAccess(any(), any()) } throws StatusException(Status.UNAVAILABLE)

            client.refundAccess(
                userId = userId,
                modelId = "gpt-4o",
                freeConsumed = 1,
                paidConsumed = 2,
            )
        }

    enum class FailingCall {
        CHECK_ACCESS,
        GET_MODEL_PREFERENCE,
    }
}
