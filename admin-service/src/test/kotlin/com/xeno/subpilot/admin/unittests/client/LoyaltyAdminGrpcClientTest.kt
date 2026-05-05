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
package com.xeno.subpilot.admin.unittests.client

import com.xeno.subpilot.admin.client.GrpcRetry
import com.xeno.subpilot.admin.client.LoyaltyAdminGrpcClient
import com.xeno.subpilot.admin.config.GrpcRetryProperties
import com.xeno.subpilot.admin.unittests.AdminTestFixtures
import com.xeno.subpilot.proto.loyalty.v1.AdjustPointsResponse
import com.xeno.subpilot.proto.loyalty.v1.GetBalanceResponse
import com.xeno.subpilot.proto.loyalty.v1.LoyaltyServiceGrpcKt
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertEquals

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class LoyaltyAdminGrpcClientTest {

    @MockK
    lateinit var stub: LoyaltyServiceGrpcKt.LoyaltyServiceCoroutineStub

    private lateinit var client: LoyaltyAdminGrpcClient

    companion object {
        const val POINTS = 350L
        const val DELTA = 50L
        const val REASON = "manual grant"
    }

    private val grpcRetry =
        GrpcRetry(
            GrpcRetryProperties(maxAttempts = 1, initialBackoffMs = 0, backoffMultiplier = 1.0),
        )

    @BeforeEach
    fun setUp() {
        client = LoyaltyAdminGrpcClient(stub, grpcRetry)
    }

    @Test
    fun `getBalance maps grpc points response`() =
        runTest {
            coEvery { stub.getBalance(any(), any()) } returns
                GetBalanceResponse
                    .newBuilder()
                    .setPoints(POINTS)
                    .build()

            val points = client.getBalance(AdminTestFixtures.USER_ID)

            assertEquals(POINTS, points)
            coVerify(exactly = 1) {
                stub.getBalance(match { it.userId == AdminTestFixtures.USER_ID }, any())
            }
        }

    @Test
    fun `adjustPoints forwards all fields to grpc`() =
        runTest {
            coEvery { stub.adjustPoints(any(), any()) } returns
                AdjustPointsResponse.getDefaultInstance()

            client.adjustPoints(
                AdminTestFixtures.USER_ID,
                DELTA,
                REASON,
                AdminTestFixtures.IDEMPOTENCY_KEY,
            )

            coVerify(exactly = 1) {
                stub.adjustPoints(
                    match {
                        it.userId == AdminTestFixtures.USER_ID &&
                            it.delta == DELTA &&
                            it.reason == REASON &&
                            it.idempotencyKey == AdminTestFixtures.IDEMPOTENCY_KEY
                    },
                    any(),
                )
            }
        }
}
