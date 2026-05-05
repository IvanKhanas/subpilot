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
package com.xeno.subpilot.payment.unittests.client

import com.xeno.subpilot.payment.client.SubscriptionGrpcClient
import com.xeno.subpilot.payment.exception.InvalidPlanException
import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import com.xeno.subpilot.proto.subscription.v1.getPlanInfoResponse
import com.xeno.subpilot.proto.subscription.v1.planInfo
import io.grpc.Status
import io.grpc.StatusException
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import java.math.BigDecimal
import java.util.stream.Stream

import kotlin.test.assertEquals

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class SubscriptionGrpcClientTest {

    @MockK(relaxed = true)
    lateinit var stub: SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineStub

    private lateinit var client: SubscriptionGrpcClient

    companion object {
        const val PLAN_ID = "openai-basic"
        const val PRICE = "199.00"
        const val CURRENCY = "RUB"

        @JvmStatic
        fun grpcFailureCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of("NOT_FOUND maps to InvalidPlanException", Status.NOT_FOUND, true),
                Arguments.of("UNAVAILABLE rethrows StatusException", Status.UNAVAILABLE, false),
            )
    }

    @BeforeEach
    fun setUp() {
        client = SubscriptionGrpcClient(stub)
    }

    @Test
    fun `getPlanDetails returns PlanDetails with price and currency from response`() =
        runTest {
            coEvery { stub.getPlanInfo(any(), any()) } returns
                getPlanInfoResponse {
                    plan =
                        planInfo {
                            planId = PLAN_ID
                            price = PRICE
                            currency = CURRENCY
                        }
                }

            val result = client.getPlanDetails(PLAN_ID)

            assertEquals(0, BigDecimal(PRICE).compareTo(result.price))
            assertEquals(CURRENCY, result.currency)
        }

    @ParameterizedTest(name = "{0}")
    @MethodSource("grpcFailureCases")
    fun `getPlanDetails maps gRPC failures`(
        caseName: String,
        status: Status,
        expectInvalidPlan: Boolean,
    ) =
        runTest {
            assertEquals(true, caseName.isNotBlank())
            coEvery { stub.getPlanInfo(any(), any()) } throws StatusException(status)

            if (expectInvalidPlan) {
                assertThrows<InvalidPlanException> {
                    client.getPlanDetails(PLAN_ID)
                }
            } else {
                assertThrows<StatusException> {
                    client.getPlanDetails(PLAN_ID)
                }
            }
        }

}
