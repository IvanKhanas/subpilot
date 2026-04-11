package com.xeno.subpilot.chat.client

import com.xeno.subpilot.chat.exception.SubscriptionServiceException
import com.xeno.subpilot.proto.subscription.v1.CheckAccessResponse
import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import com.xeno.subpilot.proto.subscription.v1.checkAccessRequest
import com.xeno.subpilot.proto.subscription.v1.getModelPreferenceRequest
import com.xeno.subpilot.proto.subscription.v1.refundAccessRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.StatusException
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class SubscriptionGrpcClient(
    private val stub: SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineStub,
) {
    suspend fun checkAccess(userId: Long, modelId: String): CheckAccessResponse =
        try {
            stub.checkAccess(
                checkAccessRequest {
                    this.userId = userId
                    this.modelId = modelId
                },
            )
        } catch (ex: StatusException) {
            throw SubscriptionServiceException("Subscription service call failed: ${ex.status}", ex)
        }

    suspend fun getModelPreference(userId: Long): String =
        try {
            stub.getModelPreference(getModelPreferenceRequest { this.userId = userId }).modelId
        } catch (ex: StatusException) {
            throw SubscriptionServiceException("Subscription service call failed: ${ex.status}", ex)
        }

    suspend fun refundAccess(userId: Long, modelId: String) {
        try {
            stub.refundAccess(refundAccessRequest {
                this.userId = userId
                this.modelId = modelId
            })
        } catch (ex: StatusException) {
            logger.atWarn {
                message = "subscription_refund_access_failed"
                cause = ex
                payload = mapOf("user_id" to userId, "model_id" to modelId)
            }
        }
    }
}
