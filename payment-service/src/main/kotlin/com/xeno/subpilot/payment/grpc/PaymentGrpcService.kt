package com.xeno.subpilot.payment.grpc

import com.xeno.subpilot.payment.client.SubscriptionGrpcClient
import com.xeno.subpilot.payment.exception.InvalidPlanException
import com.xeno.subpilot.payment.service.YooKassaPaymentService
import com.xeno.subpilot.proto.payment.v1.CreatePaymentRequest
import com.xeno.subpilot.proto.payment.v1.CreatePaymentResponse
import com.xeno.subpilot.proto.payment.v1.PaymentServiceGrpcKt
import com.xeno.subpilot.proto.payment.v1.createPaymentResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Status
import io.grpc.StatusException
import org.springframework.grpc.server.service.GrpcService

import kotlin.coroutines.CoroutineContext

import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

@GrpcService
class PaymentGrpcService(
    private val subscriptionGrpcClient: SubscriptionGrpcClient,
    private val paymentService: YooKassaPaymentService,
    private val ioDispatcher: CoroutineContext,
) : PaymentServiceGrpcKt.PaymentServiceCoroutineImplBase() {

    override suspend fun createPayment(request: CreatePaymentRequest): CreatePaymentResponse {
        logger.atDebug {
            message = "grpc_create_payment"
            payload = mapOf("user_id" to request.userId, "plan_id" to request.planId)
        }
        try {
            val plan = subscriptionGrpcClient.getPlanDetails(request.planId)
            val result =
                withContext(ioDispatcher) {
                    paymentService.createPayment(
                        request.userId,
                        request.planId,
                        request.bonusPointsToApply,
                        plan,
                    )
                }
            return createPaymentResponse {
                paymentId = result.paymentId
                confirmationUrl = result.confirmationUrl
            }
        } catch (ex: InvalidPlanException) {
            throw StatusException(
                Status.NOT_FOUND.withDescription("Unknown plan: ${request.planId}"),
            )
        } catch (ex: Exception) {
            logger.atError {
                message = "grpc_create_payment_failed"
                cause = ex
                payload = mapOf("user_id" to request.userId, "plan_id" to request.planId)
            }
            throw StatusException(Status.INTERNAL.withDescription("Payment creation failed"))
        }
    }
}
