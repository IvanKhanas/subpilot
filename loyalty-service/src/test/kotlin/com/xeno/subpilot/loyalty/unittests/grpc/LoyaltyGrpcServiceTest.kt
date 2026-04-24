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
package com.xeno.subpilot.loyalty.unittests.grpc

import com.xeno.subpilot.loyalty.dto.SpendDenialReason as ServiceSpendDenialReason
import com.xeno.subpilot.loyalty.dto.SpendResult
import com.xeno.subpilot.loyalty.grpc.LoyaltyGrpcService
import com.xeno.subpilot.loyalty.service.LoyaltyService
import com.xeno.subpilot.proto.loyalty.v1.SpendDenialReason
import com.xeno.subpilot.proto.loyalty.v1.getBalanceRequest
import com.xeno.subpilot.proto.loyalty.v1.spendPointsRequest
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

import java.util.UUID

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class LoyaltyGrpcServiceTest {

    @MockK
    lateinit var loyaltyService: LoyaltyService

    private lateinit var grpc: LoyaltyGrpcService

    companion object {
        const val USER_ID = 42L
        const val PLAN_ID = "openai-basic"
    }

    @BeforeEach
    fun setUp() {
        grpc =
            LoyaltyGrpcService(
                loyaltyService = loyaltyService,
                ioDispatcher = UnconfinedTestDispatcher(),
            )
    }

    @Test
    fun `getBalance delegates to service and returns points`() =
        runTest {
            every { loyaltyService.getBalance(USER_ID) } returns 345

            val response = grpc.getBalance(getBalanceRequest { userId = USER_ID })

            assertEquals(345, response.points)
        }

    @Test
    fun `spendPoints returns success when service returns success`() =
        runTest {
            val idempotencyKey = UUID.randomUUID()
            every { loyaltyService.spend(USER_ID, PLAN_ID, idempotencyKey) } returns
                SpendResult.Success

            val response =
                grpc.spendPoints(
                    spendPointsRequest {
                        userId = USER_ID
                        planId = PLAN_ID
                        this.idempotencyKey = idempotencyKey.toString()
                    },
                )

            assertTrue(response.success)
            verify { loyaltyService.spend(USER_ID, PLAN_ID, idempotencyKey) }
        }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        "INSUFFICIENT_POINTS, SPEND_DENIAL_REASON_INSUFFICIENT_POINTS",
        "UNKNOWN_PLAN, SPEND_DENIAL_REASON_UNKNOWN_PLAN",
    )
    fun `spendPoints maps denial reasons to proto`(
        serviceReason: ServiceSpendDenialReason,
        protoReason: SpendDenialReason,
    ) = runTest {
        val idempotencyKey = UUID.randomUUID()
        every { loyaltyService.spend(USER_ID, PLAN_ID, idempotencyKey) } returns
            SpendResult.Denied(reason = serviceReason)

        val response =
            grpc.spendPoints(
                spendPointsRequest {
                    userId = USER_ID
                    planId = PLAN_ID
                    this.idempotencyKey = idempotencyKey.toString()
                },
            )

        assertFalse(response.success)
        assertEquals(protoReason, response.denialReason)
    }
}
