package com.xeno.subpilot.loyalty.grpc

import com.xeno.subpilot.loyalty.dto.SpendDenialReason as ServiceSpendDenialReason
import com.xeno.subpilot.loyalty.dto.SpendResult
import com.xeno.subpilot.loyalty.service.LoyaltyService
import com.xeno.subpilot.proto.loyalty.v1.GetBalanceRequest
import com.xeno.subpilot.proto.loyalty.v1.GetBalanceResponse
import com.xeno.subpilot.proto.loyalty.v1.LoyaltyServiceGrpcKt
import com.xeno.subpilot.proto.loyalty.v1.SpendDenialReason
import com.xeno.subpilot.proto.loyalty.v1.SpendPointsRequest
import com.xeno.subpilot.proto.loyalty.v1.SpendPointsResponse
import com.xeno.subpilot.proto.loyalty.v1.getBalanceResponse
import com.xeno.subpilot.proto.loyalty.v1.spendPointsResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.grpc.server.service.GrpcService

import java.util.UUID

import kotlin.coroutines.CoroutineContext

import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

@GrpcService
class LoyaltyGrpcService(
    private val loyaltyService: LoyaltyService,
    private val ioDispatcher: CoroutineContext,
) : LoyaltyServiceGrpcKt.LoyaltyServiceCoroutineImplBase() {

    override suspend fun getBalance(request: GetBalanceRequest): GetBalanceResponse {
        logger.atDebug {
            message = "grpc_get_balance"
            payload = mapOf("user_id" to request.userId)
        }
        val balance =
            withContext(ioDispatcher) {
                loyaltyService.getBalance(request.userId)
            }
        return getBalanceResponse { points = balance }
    }

    override suspend fun spendPoints(request: SpendPointsRequest): SpendPointsResponse {
        logger.atDebug {
            message = "grpc_spend_points"
            payload = mapOf("user_id" to request.userId, "plan_id" to request.planId)
        }
        val idempotencyKey = UUID.fromString(request.idempotencyKey)
        val result =
            withContext(ioDispatcher) {
                loyaltyService.spend(request.userId, request.planId, idempotencyKey)
            }
        return spendPointsResponse {
            when (result) {
                is SpendResult.Success -> success = true
                is SpendResult.Denied -> {
                    success = false
                    denialReason = result.reason.toProto()
                }
            }
        }
    }

    private fun ServiceSpendDenialReason.toProto(): SpendDenialReason =
        when (this) {
            ServiceSpendDenialReason.INSUFFICIENT_POINTS ->
                SpendDenialReason.SPEND_DENIAL_REASON_INSUFFICIENT_POINTS
            ServiceSpendDenialReason.UNKNOWN_PLAN ->
                SpendDenialReason.SPEND_DENIAL_REASON_UNKNOWN_PLAN
        }
}
