package com.xeno.subpilot.tgbot.client

import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import com.xeno.subpilot.proto.subscription.v1.registerUserRequest
import com.xeno.subpilot.proto.subscription.v1.setModelPreferenceRequest
import com.xeno.subpilot.tgbot.exception.SubscriptionServiceException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.StatusException
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class SubscriptionGrpcClient(
    private val stub: SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineStub,
) {
    fun registerUser(userId: Long) {
        try {
            runBlocking {
                stub.registerUser(registerUserRequest { this.userId = userId })
            }
        } catch (ex: StatusException) {
            logger.atWarn {
                message = "subscription_register_user_failed"
                cause = ex
                payload = mapOf("user_id" to userId)
            }
        }
    }

    fun setModelPreference(
        userId: Long,
        modelId: String,
    ) {
        try {
            runBlocking {
                stub.setModelPreference(setModelPreferenceRequest {
                    this.userId = userId
                    this.modelId = modelId
                })
            }
        } catch (ex: StatusException) {
            logger.atWarn {
                message = "subscription_set_model_preference_failed"
                cause = ex
                payload = mapOf("user_id" to userId, "model_id" to modelId)
            }
            throw SubscriptionServiceException("Failed to set model preference", ex)
        }
    }
}
