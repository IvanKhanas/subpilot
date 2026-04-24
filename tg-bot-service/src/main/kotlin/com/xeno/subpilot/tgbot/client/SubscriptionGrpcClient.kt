package com.xeno.subpilot.tgbot.client

import com.xeno.subpilot.proto.subscription.v1.GetPlansRequest
import com.xeno.subpilot.proto.subscription.v1.PlanInfo as ProtoPlanInfo
import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import com.xeno.subpilot.proto.subscription.v1.getBalanceRequest
import com.xeno.subpilot.proto.subscription.v1.getPlanInfoRequest
import com.xeno.subpilot.proto.subscription.v1.registerUserRequest
import com.xeno.subpilot.proto.subscription.v1.setModelPreferenceRequest
import com.xeno.subpilot.tgbot.dto.BalanceInfo
import com.xeno.subpilot.tgbot.dto.FreeProviderBalance
import com.xeno.subpilot.tgbot.dto.ModelPreferenceResult
import com.xeno.subpilot.tgbot.dto.PaidProviderBalance
import com.xeno.subpilot.tgbot.dto.PlanAllocation
import com.xeno.subpilot.tgbot.dto.PlanInfo
import com.xeno.subpilot.tgbot.dto.RegistrationResult
import com.xeno.subpilot.tgbot.exception.SubscriptionServiceException
import com.xeno.subpilot.tgbot.ux.AiProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Status
import io.grpc.StatusException
import org.springframework.stereotype.Component

import java.time.Instant

private val logger = KotlinLogging.logger {}

@Component
class SubscriptionGrpcClient(
    private val stub: SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineStub,
    private val grpcRetry: GrpcRetry,
) : SubscriptionClient {
    override suspend fun registerUser(userId: Long): RegistrationResult? =
        try {
            val response =
                grpcRetry.retryOnUnavailable {
                    stub.registerUser(registerUserRequest { this.userId = userId })
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

    override suspend fun setModelPreference(
        userId: Long,
        modelId: String,
    ): ModelPreferenceResult {
        try {
            val response =
                grpcRetry.retryOnUnavailable {
                    stub.setModelPreference(
                        setModelPreferenceRequest {
                            this.userId = userId
                            this.modelId = modelId
                        },
                    )
                }
            return ModelPreferenceResult(
                providerChanged = response.providerChanged,
                modelCost = response.modelCost,
                provider = response.provider,
            )
        } catch (ex: StatusException) {
            logger.atWarn {
                message = "subscription_set_model_preference_failed"
                cause = ex
                payload = mapOf("user_id" to userId, "model_id" to modelId)
            }
            throw SubscriptionServiceException("Failed to set model preference", ex)
        }
    }

    override suspend fun getPlans(): List<PlanInfo> {
        try {
            val response =
                grpcRetry.retryOnUnavailable {
                    stub.getPlans(GetPlansRequest.getDefaultInstance())
                }
            return response.plansList.map { planProtoToDto(it) }
        } catch (ex: StatusException) {
            logger.atError {
                message = "subscription_get_plans_failed"
                cause = ex
            }
            throw SubscriptionServiceException("Failed to get plans", ex)
        }
    }

    override suspend fun getPlanInfo(planId: String): PlanInfo? {
        try {
            val response =
                grpcRetry.retryOnUnavailable {
                    stub.getPlanInfo(getPlanInfoRequest { this.planId = planId })
                }
            return planProtoToDto(response.plan)
        } catch (ex: StatusException) {
            if (ex.status.code == Status.Code.NOT_FOUND) return null
            logger.atError {
                message = "subscription_get_plan_info_failed"
                cause = ex
                payload = mapOf("plan_id" to planId)
            }
            throw SubscriptionServiceException("Failed to get plan info", ex)
        }
    }

    override suspend fun getBalance(userId: Long): BalanceInfo {
        try {
            val response =
                grpcRetry.retryOnUnavailable {
                    stub.getBalance(getBalanceRequest { this.userId = userId })
                }
            return BalanceInfo(
                freeBalances =
                    response.freeBalancesList.map { b ->
                        FreeProviderBalance(
                            provider = b.provider,
                            requestsRemaining = b.requestsRemaining,
                            nextResetAt = Instant.ofEpochSecond(b.nextResetAtEpoch),
                        )
                    },
                paidBalances =
                    response.paidBalancesList.map { b ->
                        PaidProviderBalance(
                            provider = b.provider,
                            requestsRemaining = b.requestsRemaining,
                        )
                    },
            )
        } catch (ex: StatusException) {
            logger.atError {
                message = "subscription_get_balance_failed"
                cause = ex
                payload = mapOf("user_id" to userId)
            }
            throw SubscriptionServiceException("Failed to get balance", ex)
        }
    }

    private fun planProtoToDto(plan: ProtoPlanInfo): PlanInfo =
        PlanInfo(
            planId = plan.planId,
            provider = plan.provider,
            displayName = plan.displayName,
            price = plan.price,
            currency = plan.currency,
            allocations = plan.allocationsList.map { PlanAllocation(it.provider, it.requests) },
        )
}
