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
package com.xeno.subpilot.loyalty.unittests.client

import com.xeno.subpilot.loyalty.client.GrpcRetry
import com.xeno.subpilot.loyalty.client.SubscriptionGrpcClient
import com.xeno.subpilot.loyalty.config.GrpcRetryProperties
import com.xeno.subpilot.loyalty.exception.SubscriptionServiceException
import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import com.xeno.subpilot.proto.subscription.v1.activateSubscriptionResponse
import com.xeno.subpilot.proto.subscription.v1.getPlanInfoResponse
import com.xeno.subpilot.proto.subscription.v1.planInfo
import io.grpc.Status
import io.grpc.StatusException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

import java.util.UUID

import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(MockKExtension::class)
class SubscriptionGrpcClientTest {

    @MockK(relaxed = true)
    lateinit var stub: SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineStub

    private lateinit var client: SubscriptionGrpcClient

    companion object {
        const val USER_ID = 42L
        const val PLAN_ID = "openai-basic"
    }

    @BeforeEach
    fun setUp() {
        client =
            SubscriptionGrpcClient(
                stub = stub,
                grpcRetry =
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
    fun `getPlanInfo returns plan from grpc response`() {
        coEvery { stub.getPlanInfo(any(), any()) } returns
            getPlanInfoResponse {
                plan =
                    planInfo {
                        this.planId = PLAN_ID
                        price = "199.00"
                        currency = "RUB"
                    }
            }

        val result = client.getPlanInfo(PLAN_ID)

        assertEquals(PLAN_ID, result?.planId)
        assertEquals("199.00", result?.price)
        assertEquals("RUB", result?.currency)
    }

    @Test
    fun `getPlanInfo returns null on NOT_FOUND`() {
        coEvery { stub.getPlanInfo(any(), any()) } throws StatusException(Status.NOT_FOUND)

        val result = client.getPlanInfo(PLAN_ID)

        assertNull(result)
    }

    @ParameterizedTest
    @EnumSource(
        value = Status.Code::class,
        names = ["UNAVAILABLE", "INTERNAL", "PERMISSION_DENIED"],
    )
    fun `getPlanInfo throws SubscriptionServiceException on non-NOT_FOUND statuses`(
        statusCode: Status.Code,
    ) {
        coEvery { stub.getPlanInfo(any(), any()) } throws
            StatusException(Status.fromCode(statusCode))

        assertThrows<SubscriptionServiceException> {
            client.getPlanInfo(PLAN_ID)
        }
    }

    @Test
    fun `activateSubscription delegates to grpc stub`() {
        coEvery { stub.activateSubscription(any(), any()) } returns activateSubscriptionResponse {}
        val idempotencyKey = UUID.randomUUID()

        client.activateSubscription(USER_ID, PLAN_ID, idempotencyKey)

        coVerify(exactly = 1) { stub.activateSubscription(any(), any()) }
    }

    @Test
    fun `activateSubscription wraps grpc status exceptions`() {
        coEvery { stub.activateSubscription(any(), any()) } throws
            StatusException(Status.UNAVAILABLE)

        assertThrows<SubscriptionServiceException> {
            client.activateSubscription(USER_ID, PLAN_ID, UUID.randomUUID())
        }
    }
}
