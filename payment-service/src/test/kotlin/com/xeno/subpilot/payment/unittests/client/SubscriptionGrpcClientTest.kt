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

import java.math.BigDecimal

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

    @Test
    fun `getPlanDetails throws InvalidPlanException on NOT_FOUND`() =
        runTest {
            coEvery { stub.getPlanInfo(any(), any()) } throws StatusException(Status.NOT_FOUND)

            assertThrows<InvalidPlanException> {
                client.getPlanDetails(PLAN_ID)
            }
        }

    @Test
    fun `getPlanDetails rethrows StatusException on other gRPC errors`() =
        runTest {
            coEvery { stub.getPlanInfo(any(), any()) } throws StatusException(Status.UNAVAILABLE)

            assertThrows<StatusException> {
                client.getPlanDetails(PLAN_ID)
            }
        }
}
