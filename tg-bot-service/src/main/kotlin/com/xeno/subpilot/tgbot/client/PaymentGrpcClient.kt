package com.xeno.subpilot.tgbot.client

import com.xeno.subpilot.proto.payment.v1.PaymentServiceGrpcKt
import com.xeno.subpilot.proto.payment.v1.createPaymentRequest
import com.xeno.subpilot.tgbot.exception.PaymentServiceException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.StatusException
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class PaymentGrpcClient(
    private val stub: PaymentServiceGrpcKt.PaymentServiceCoroutineStub,
    private val grpcRetry: GrpcRetry,
) : PaymentClient {

    override suspend fun createPayment(
        userId: Long,
        planId: String,
        bonusPointsToApply: Long,
    ): String {
        try {
            return grpcRetry.retryOnUnavailable {
                stub
                    .createPayment(
                        createPaymentRequest {
                            this.userId = userId
                            this.planId = planId
                            this.bonusPointsToApply = bonusPointsToApply
                        },
                    ).confirmationUrl
            }
        } catch (ex: StatusException) {
            logger.atError {
                message = "payment_create_payment_failed"
                cause = ex
                payload = mapOf("user_id" to userId, "plan_id" to planId)
            }
            throw PaymentServiceException("Failed to create payment", ex)
        }
    }
}
