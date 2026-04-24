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
package com.xeno.subpilot.subscription.grpc

import com.xeno.subpilot.proto.subscription.v1.ActivateSubscriptionRequest
import com.xeno.subpilot.proto.subscription.v1.ActivateSubscriptionResponse
import com.xeno.subpilot.proto.subscription.v1.CheckAccessRequest
import com.xeno.subpilot.proto.subscription.v1.CheckAccessResponse
import com.xeno.subpilot.proto.subscription.v1.DenialReason
import com.xeno.subpilot.proto.subscription.v1.GetBalanceRequest
import com.xeno.subpilot.proto.subscription.v1.GetBalanceResponse
import com.xeno.subpilot.proto.subscription.v1.GetModelPreferenceRequest
import com.xeno.subpilot.proto.subscription.v1.GetModelPreferenceResponse
import com.xeno.subpilot.proto.subscription.v1.GetPlanInfoRequest
import com.xeno.subpilot.proto.subscription.v1.GetPlanInfoResponse
import com.xeno.subpilot.proto.subscription.v1.GetPlansRequest
import com.xeno.subpilot.proto.subscription.v1.GetPlansResponse
import com.xeno.subpilot.proto.subscription.v1.PlanAllocation
import com.xeno.subpilot.proto.subscription.v1.PlanInfo
import com.xeno.subpilot.proto.subscription.v1.ProviderBalance as ProtoProviderBalance
import com.xeno.subpilot.proto.subscription.v1.RefundAccessRequest
import com.xeno.subpilot.proto.subscription.v1.RefundAccessResponse
import com.xeno.subpilot.proto.subscription.v1.RegisterUserRequest
import com.xeno.subpilot.proto.subscription.v1.RegisterUserResponse
import com.xeno.subpilot.proto.subscription.v1.SetModelPreferenceRequest
import com.xeno.subpilot.proto.subscription.v1.SetModelPreferenceResponse
import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import com.xeno.subpilot.proto.subscription.v1.activateSubscriptionResponse
import com.xeno.subpilot.proto.subscription.v1.checkAccessResponse
import com.xeno.subpilot.proto.subscription.v1.getModelPreferenceResponse
import com.xeno.subpilot.proto.subscription.v1.getPlanInfoResponse
import com.xeno.subpilot.proto.subscription.v1.refundAccessResponse
import com.xeno.subpilot.proto.subscription.v1.registerUserResponse
import com.xeno.subpilot.proto.subscription.v1.setModelPreferenceResponse
import com.xeno.subpilot.subscription.dto.DenialReason as ServiceDenialReason
import com.xeno.subpilot.subscription.dto.FreeProviderBalance as ServiceFreeProviderBalance
import com.xeno.subpilot.subscription.dto.PaidProviderBalance as ServicePaidProviderBalance
import com.xeno.subpilot.subscription.properties.PlanProperties
import com.xeno.subpilot.subscription.properties.SubscriptionProperties
import com.xeno.subpilot.subscription.repository.PlanRepository
import com.xeno.subpilot.subscription.service.AccessService
import com.xeno.subpilot.subscription.service.BalanceService
import com.xeno.subpilot.subscription.service.ModelPreferenceService
import com.xeno.subpilot.subscription.service.SubscriptionActivationService
import com.xeno.subpilot.subscription.service.UserService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Status
import io.grpc.StatusException
import org.springframework.grpc.server.service.GrpcService

import java.time.ZoneOffset
import java.util.UUID

import kotlin.coroutines.CoroutineContext

import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

@GrpcService
class SubscriptionGrpcService(
    private val accessService: AccessService,
    private val userService: UserService,
    private val modelPreferenceService: ModelPreferenceService,
    private val balanceService: BalanceService,
    private val activationService: SubscriptionActivationService,
    private val subscriptionProperties: SubscriptionProperties,
    private val planRepository: PlanRepository,
    private val ioDispatcher: CoroutineContext,
) : SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineImplBase() {

    override suspend fun refundAccess(request: RefundAccessRequest): RefundAccessResponse {
        logger.atDebug {
            message = "grpc_refund_access"
            payload = mapOf("user_id" to request.userId, "model_id" to request.modelId)
        }
        withContext(ioDispatcher) {
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
            withContext(ioDispatcher) {
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
            withContext(ioDispatcher) {
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
            withContext(ioDispatcher) {
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
        val result =
            withContext(ioDispatcher) {
                modelPreferenceService.setModelPreference(request.userId, request.modelId)
            }
        return setModelPreferenceResponse {
            providerChanged = result.providerChanged
            modelCost = result.modelCost
            provider = result.provider
        }
    }

    override suspend fun getPlans(request: GetPlansRequest): GetPlansResponse {
        logger.atDebug { message = "grpc_get_plans" }
        val planInfoList =
            withContext(ioDispatcher) { planRepository.findAllActive() }
                .map { (id, planConfig) -> planConfigToProto(id, planConfig) }
        return GetPlansResponse.newBuilder().addAllPlans(planInfoList).build()
    }

    override suspend fun getPlanInfo(request: GetPlanInfoRequest): GetPlanInfoResponse {
        logger.atDebug {
            message = "grpc_get_plan_info"
            payload = mapOf("plan_id" to request.planId)
        }
        val planConfig =
            withContext(ioDispatcher) { planRepository.findById(request.planId) }
                ?: throw StatusException(
                    Status.NOT_FOUND.withDescription("Unknown plan: ${request.planId}"),
                )
        return getPlanInfoResponse {
            plan = planConfigToProto(request.planId, planConfig)
        }
    }

    private fun planConfigToProto(
        id: String,
        planConfig: PlanProperties,
    ): PlanInfo =
        PlanInfo
            .newBuilder()
            .setPlanId(id)
            .setProvider(planConfig.provider)
            .setDisplayName(planConfig.displayName)
            .setPrice(planConfig.price.toPlainString())
            .setCurrency(planConfig.currency)
            .addAllAllocations(
                planConfig.allocations.map { alloc ->
                    PlanAllocation
                        .newBuilder()
                        .setProvider(alloc.provider)
                        .setRequests(alloc.requests)
                        .build()
                },
            ).build()

    private fun ServiceDenialReason.toProto(): DenialReason =
        when (this) {
            ServiceDenialReason.QUOTA_EXHAUSTED -> DenialReason.QUOTA_EXHAUSTED
            ServiceDenialReason.NO_SUBSCRIPTION -> DenialReason.NO_SUBSCRIPTION
            ServiceDenialReason.BLOCKED -> DenialReason.BLOCKED
            ServiceDenialReason.UNSPECIFIED -> DenialReason.DENIAL_REASON_UNSPECIFIED
        }

    override suspend fun getBalance(request: GetBalanceRequest): GetBalanceResponse {
        logger.atDebug {
            message = "grpc_get_balance"
            payload = mapOf("user_id" to request.userId)
        }
        val balanceInfo =
            withContext(ioDispatcher) {
                balanceService.getBalance(request.userId)
            }
        return GetBalanceResponse
            .newBuilder()
            .addAllFreeBalances(balanceInfo.freeBalances.map(::freeBalanceToProto))
            .addAllPaidBalances(balanceInfo.paidBalances.map(::paidBalanceToProto))
            .build()
    }

    override suspend fun activateSubscription(
        request: ActivateSubscriptionRequest,
    ): ActivateSubscriptionResponse {
        logger.atDebug {
            message = "grpc_activate_subscription"
            payload = mapOf("user_id" to request.userId, "plan_id" to request.planId)
        }
        withContext(ioDispatcher) {
            activationService.activateDirect(
                userId = request.userId,
                planId = request.planId,
                idempotencyKey = UUID.fromString(request.idempotencyKey),
            )
        }
        return activateSubscriptionResponse { }
    }

    private fun freeBalanceToProto(balance: ServiceFreeProviderBalance) =
        ProtoProviderBalance
            .newBuilder()
            .setProvider(balance.provider)
            .setRequestsRemaining(balance.requestsRemaining)
            .setNextResetAtEpoch(balance.nextResetAt.toEpochSecond(ZoneOffset.UTC))
            .build()

    private fun paidBalanceToProto(balance: ServicePaidProviderBalance) =
        ProtoProviderBalance
            .newBuilder()
            .setProvider(balance.provider)
            .setRequestsRemaining(balance.requestsRemaining)
            .build()
}
