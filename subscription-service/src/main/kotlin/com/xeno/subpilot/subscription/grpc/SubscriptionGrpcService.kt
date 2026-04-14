package com.xeno.subpilot.subscription.grpc

import com.xeno.subpilot.proto.subscription.v1.CheckAccessRequest
import com.xeno.subpilot.proto.subscription.v1.CheckAccessResponse
import com.xeno.subpilot.proto.subscription.v1.DenialReason
import com.xeno.subpilot.proto.subscription.v1.GetModelPreferenceRequest
import com.xeno.subpilot.proto.subscription.v1.GetModelPreferenceResponse
import com.xeno.subpilot.proto.subscription.v1.RefundAccessRequest
import com.xeno.subpilot.proto.subscription.v1.RefundAccessResponse
import com.xeno.subpilot.proto.subscription.v1.RegisterUserRequest
import com.xeno.subpilot.proto.subscription.v1.RegisterUserResponse
import com.xeno.subpilot.proto.subscription.v1.SetModelPreferenceRequest
import com.xeno.subpilot.proto.subscription.v1.SetModelPreferenceResponse
import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import com.xeno.subpilot.proto.subscription.v1.checkAccessResponse
import com.xeno.subpilot.proto.subscription.v1.getModelPreferenceResponse
import com.xeno.subpilot.proto.subscription.v1.refundAccessResponse
import com.xeno.subpilot.proto.subscription.v1.registerUserResponse
import com.xeno.subpilot.proto.subscription.v1.setModelPreferenceResponse
import com.xeno.subpilot.subscription.dto.DenialReason as ServiceDenialReason
import com.xeno.subpilot.subscription.properties.SubscriptionProperties
import com.xeno.subpilot.subscription.service.AccessService
import com.xeno.subpilot.subscription.service.ModelPreferenceService
import com.xeno.subpilot.subscription.service.UserService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.grpc.server.service.GrpcService

import java.time.ZoneOffset

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

@GrpcService
class SubscriptionGrpcService(
    private val accessService: AccessService,
    private val userService: UserService,
    private val modelPreferenceService: ModelPreferenceService,
    private val subscriptionProperties: SubscriptionProperties,
) : SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineImplBase() {

    override suspend fun refundAccess(request: RefundAccessRequest): RefundAccessResponse {
        logger.atDebug {
            message = "grpc_refund_access"
            payload = mapOf("user_id" to request.userId, "model_id" to request.modelId)
        }
        withContext(Dispatchers.IO) {
            accessService.refund(
                request.userId,
                request.modelId,
                request.freeConsumed,
                request.paidConsumed,
            )
        }
        return refundAccessResponse { }
    }

    override suspend fun checkAccess(request: CheckAccessRequest): CheckAccessResponse {
        logger.atDebug {
            message = "grpc_check_access"
            payload = mapOf("user_id" to request.userId, "model_id" to request.modelId)
        }
        val result =
            withContext(Dispatchers.IO) {
                accessService.checkAndConsume(request.userId, request.modelId)
            }
        return checkAccessResponse {
            allowed = result.allowed
            denialReason = result.denialReason.toProto()
            availableRequests = result.availableRequests
            modelCost = result.modelCost
            freeConsumed = result.freeConsumed
            paidConsumed = result.paidConsumed
            if (result.resetAt != null) {
                resetAtEpoch = result.resetAt.toEpochSecond(ZoneOffset.UTC)
            }
        }
    }

    override suspend fun registerUser(request: RegisterUserRequest): RegisterUserResponse {
        logger.atDebug {
            message = "grpc_register_user"
            payload = mapOf("user_id" to request.userId)
        }
        val isNew =
            withContext(Dispatchers.IO) {
                userService.registerUser(request.userId)
            }
        return registerUserResponse {
            this.isNew = isNew
            freeQuota = subscriptionProperties.freeQuota
            freeModelId = subscriptionProperties.defaultModel
        }
    }

    override suspend fun getModelPreference(
        request: GetModelPreferenceRequest,
    ): GetModelPreferenceResponse {
        logger.atDebug {
            message = "grpc_get_model_preference"
            payload = mapOf("user_id" to request.userId)
        }
        val modelId =
            withContext(Dispatchers.IO) {
                modelPreferenceService.getModelPreference(request.userId)
            }
        return getModelPreferenceResponse {
            if (modelId != null) this.modelId = modelId
        }
    }

    override suspend fun setModelPreference(
        request: SetModelPreferenceRequest,
    ): SetModelPreferenceResponse {
        logger.atDebug {
            message = "grpc_set_model_preference"
            payload = mapOf("user_id" to request.userId, "model_id" to request.modelId)
        }
        val providerChanged =
            withContext(Dispatchers.IO) {
                modelPreferenceService.setModelPreference(request.userId, request.modelId)
            }
        return setModelPreferenceResponse { this.providerChanged = providerChanged }
    }

    private fun ServiceDenialReason.toProto(): DenialReason =
        when (this) {
            ServiceDenialReason.QUOTA_EXHAUSTED -> DenialReason.QUOTA_EXHAUSTED
            ServiceDenialReason.NO_SUBSCRIPTION -> DenialReason.NO_SUBSCRIPTION
            ServiceDenialReason.BLOCKED -> DenialReason.BLOCKED
            ServiceDenialReason.UNSPECIFIED -> DenialReason.DENIAL_REASON_UNSPECIFIED
        }
}
