package com.xeno.subpilot.payment.unittests.grpc

import com.xeno.subpilot.payment.client.SubscriptionGrpcClient
import com.xeno.subpilot.payment.dto.PaymentResult
import com.xeno.subpilot.payment.dto.PlanDetails
import com.xeno.subpilot.payment.exception.InvalidPlanException
import com.xeno.subpilot.payment.grpc.PaymentGrpcService
import com.xeno.subpilot.payment.service.YooKassaPaymentService
import com.xeno.subpilot.proto.payment.v1.createPaymentRequest
import io.grpc.Status
import io.grpc.StatusException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

import java.math.BigDecimal

import kotlin.test.assertEquals

import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class PaymentGrpcServiceTest {

    @MockK
    lateinit var subscriptionGrpcClient: SubscriptionGrpcClient

    @MockK
    lateinit var paymentService: YooKassaPaymentService

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
    fun `createPayment throws StatusException with NOT_FOUND on InvalidPlanException`() =
        runTest {
            coEvery { subscriptionGrpcClient.getPlanDetails(PLAN_ID) } throws
                InvalidPlanException(PLAN_ID)

            val ex =
                assertThrows<StatusException> {
                    grpc.createPayment(request(bonusPoints = 0))
                }

            assertEquals(Status.Code.NOT_FOUND, ex.status.code)
        }

    @Test
    fun `createPayment throws StatusException with INTERNAL on unexpected exception`() =
        runTest {
            coEvery { subscriptionGrpcClient.getPlanDetails(PLAN_ID) } returns PLAN
            every { paymentService.createPayment(any(), any(), any(), any()) } throws
                RuntimeException("db down")

            val ex =
                assertThrows<StatusException> {
                    grpc.createPayment(request(bonusPoints = 0))
                }

            assertEquals(Status.Code.INTERNAL, ex.status.code)
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
