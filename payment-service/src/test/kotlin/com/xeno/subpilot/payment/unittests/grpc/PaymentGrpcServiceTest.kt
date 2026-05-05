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
package com.xeno.subpilot.payment.unittests.grpc

import com.xeno.subpilot.payment.client.SubscriptionClient
import com.xeno.subpilot.payment.dto.PaymentResult
import com.xeno.subpilot.payment.dto.PlanDetails
import com.xeno.subpilot.payment.exception.InvalidPlanException
import com.xeno.subpilot.payment.grpc.PaymentGrpcService
import com.xeno.subpilot.payment.service.YooKassaPaymentService
import com.xeno.subpilot.payment.service.kafka.YooKassaPaymentOutboxPublisher
import com.xeno.subpilot.proto.payment.v1.createPaymentRequest
import com.xeno.subpilot.proto.payment.v1.triggerOutboxFlushRequest
import io.grpc.Status
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

import java.math.BigDecimal

import kotlin.test.assertEquals

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class PaymentGrpcServiceTest {

    @MockK
    lateinit var subscriptionGrpcClient: SubscriptionClient

    @MockK
    lateinit var paymentService: YooKassaPaymentService

    @MockK(relaxed = true)
    lateinit var outboxPublisher: YooKassaPaymentOutboxPublisher

    private lateinit var grpc: PaymentGrpcService

    companion object {
        const val USER_ID = 42L
        const val PLAN_ID = "openai-basic"
        const val PAYMENT_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        const val CONFIRMATION_URL = "https://yookassa.ru/checkout/payments/test"
        val PLAN = PlanDetails(price = BigDecimal("199.00"), currency = "RUB")
    }

    @BeforeEach
    fun setUp() {
        grpc =
            PaymentGrpcService(
                subscriptionGrpcClient = subscriptionGrpcClient,
                paymentService = paymentService,
                outboxPublisher = outboxPublisher,
                ioDispatcher = UnconfinedTestDispatcher(),
            )
    }

    @Test
    fun `createPayment returns paymentId and confirmationUrl on success`() =
        runTest {
            coEvery { subscriptionGrpcClient.getPlanDetails(PLAN_ID) } returns PLAN
            every { paymentService.createPayment(USER_ID, PLAN_ID, 0L, PLAN) } returns
                paymentResult()

            val response = grpc.createPayment(request(bonusPoints = 0))

            assertEquals(PAYMENT_ID, response.paymentId)
            assertEquals(CONFIRMATION_URL, response.confirmationUrl)
        }

    @Test
    fun `createPayment passes bonusPointsToApply to payment service`() =
        runTest {
            coEvery { subscriptionGrpcClient.getPlanDetails(PLAN_ID) } returns PLAN
            every { paymentService.createPayment(USER_ID, PLAN_ID, 50L, PLAN) } returns
                paymentResult()

            grpc.createPayment(request(bonusPoints = 50))

            coVerify { subscriptionGrpcClient.getPlanDetails(PLAN_ID) }
        }

    @Test
    fun `createPayment throws InvalidPlanException when plan does not exist`() =
        runTest {
            coEvery { subscriptionGrpcClient.getPlanDetails(PLAN_ID) } throws
                InvalidPlanException(PLAN_ID)

            val ex =
                assertThrows<InvalidPlanException> {
                    grpc.createPayment(request(bonusPoints = 0))
                }

            assertEquals(Status.Code.NOT_FOUND, ex.status.code)
        }

    @Test
    fun `createPayment propagates unexpected exception for handler to convert`() =
        runTest {
            coEvery { subscriptionGrpcClient.getPlanDetails(PLAN_ID) } returns PLAN
            every { paymentService.createPayment(any(), any(), any(), any()) } throws
                RuntimeException("db down")

            assertThrows<RuntimeException> {
                grpc.createPayment(request(bonusPoints = 0))
            }
        }

    @Test
    fun `triggerOutboxFlush returns flushed event count from outbox publisher`() =
        runTest {
            every { outboxPublisher.publishPending() } returns 3

            val response = grpc.triggerOutboxFlush(triggerOutboxFlushRequest { })

            assertEquals(3, response.flushedCount)
            verify(exactly = 1) { outboxPublisher.publishPending() }
        }

    private fun request(bonusPoints: Long) =
        createPaymentRequest {
            userId = USER_ID
            planId = PLAN_ID
            bonusPointsToApply = bonusPoints
        }

    private fun paymentResult() =
        PaymentResult(paymentId = PAYMENT_ID, confirmationUrl = CONFIRMATION_URL)
}
