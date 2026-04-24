package com.xeno.subpilot.tgbot.client

import com.xeno.subpilot.proto.loyalty.v1.LoyaltyServiceGrpcKt
import com.xeno.subpilot.proto.loyalty.v1.SpendDenialReason as ProtoSpendDenialReason
import com.xeno.subpilot.proto.loyalty.v1.getBalanceRequest
import com.xeno.subpilot.proto.loyalty.v1.spendPointsRequest
import com.xeno.subpilot.tgbot.dto.SpendDenialReason
import com.xeno.subpilot.tgbot.dto.SpendResult
import com.xeno.subpilot.tgbot.exception.LoyaltyServiceException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.StatusException
import org.springframework.stereotype.Component

import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class LoyaltyGrpcClient(
    private val stub: LoyaltyServiceGrpcKt.LoyaltyServiceCoroutineStub,
    private val grpcRetry: GrpcRetry,
) : LoyaltyClient {

    override suspend fun getBalance(userId: Long): Long =
        try {
            grpcRetry.retryOnUnavailable {
                stub.getBalance(getBalanceRequest { this.userId = userId }).points
            }
        } catch (ex: StatusException) {
            logger.atError {
                message = "loyalty_get_balance_failed"
                cause = ex
                payload = mapOf("user_id" to userId)
            }
            throw LoyaltyServiceException("Failed to get loyalty balance", ex)
        }

    override suspend fun spend(
        userId: Long,
        planId: String,
        idempotencyKey: UUID,
    ): SpendResult {
        val response =
            try {
                stub.spendPoints(
                    spendPointsRequest {
                        this.userId = userId
                        this.planId = planId
                        this.idempotencyKey = idempotencyKey.toString()
                    },
                )
            } catch (ex: StatusException) {
                logger.atError {
                    message = "loyalty_spend_points_failed"
                    cause = ex
                    payload = mapOf("user_id" to userId, "plan_id" to planId)
                }
                throw LoyaltyServiceException("Failed to spend loyalty points", ex)
            }
        return if (response.success) {
            SpendResult.Success
        } else {
            SpendResult.Denied(response.denialReason.toDto())
        }
    }

    private fun ProtoSpendDenialReason.toDto(): SpendDenialReason =
        when (this) {
            ProtoSpendDenialReason.SPEND_DENIAL_REASON_INSUFFICIENT_POINTS ->
                SpendDenialReason.INSUFFICIENT_POINTS
            ProtoSpendDenialReason.SPEND_DENIAL_REASON_UNKNOWN_PLAN ->
                SpendDenialReason.UNKNOWN_PLAN
            else -> SpendDenialReason.UNSPECIFIED
        }
}
