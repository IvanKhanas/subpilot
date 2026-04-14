package com.xeno.subpilot.tgbot.client

import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import com.xeno.subpilot.proto.subscription.v1.registerUserRequest
import com.xeno.subpilot.proto.subscription.v1.setModelPreferenceRequest
import com.xeno.subpilot.tgbot.dto.RegistrationResult
import com.xeno.subpilot.tgbot.exception.SubscriptionServiceException
import com.xeno.subpilot.tgbot.ux.AiProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.StatusException
import org.springframework.stereotype.Component

import kotlinx.coroutines.runBlocking

private val logger = KotlinLogging.logger {}

@Component
class SubscriptionGrpcClient(
    private val stub: SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineStub,
    private val grpcRetry: GrpcRetry,
) : SubscriptionClient {
    override fun registerUser(userId: Long): RegistrationResult? =
        try {
            val response =
                runBlocking {
                    grpcRetry.retryOnUnavailable {
                        stub.registerUser(registerUserRequest { this.userId = userId })
                    }
                }
            val freeProvider =
                AiProvider.findProviderByModelId(response.freeModelId)
                    ?: error(
                        "Unknown freeModelId from subscription-service: ${response.freeModelId}",
                    )
            RegistrationResult(
                isNew = response.isNew,
                freeQuota = response.freeQuota,
                freeProvider = freeProvider,
            )
        } catch (ex: StatusException) {
            logger.atWarn {
                message = "subscription_register_user_failed"
                cause = ex
                payload = mapOf("user_id" to userId)
            }
            null
        }

    override fun setModelPreference(
        userId: Long,
        modelId: String,
    ): Boolean {
        try {
            return runBlocking {
                grpcRetry.retryOnUnavailable {
                    stub
                        .setModelPreference(
                            setModelPreferenceRequest {
                                this.userId = userId
                                this.modelId = modelId
                            },
                        ).providerChanged
                }
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
