/*
 * Copyright 2026 Ivan Khanas
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
package com.xeno.subpilot.admin.service

import com.xeno.subpilot.admin.client.LoyaltyAdminClient
import com.xeno.subpilot.admin.client.SubscriptionAdminClient
import com.xeno.subpilot.admin.dto.AddSubscriptionRequest
import com.xeno.subpilot.admin.dto.AdjustLoyaltyRequest
import com.xeno.subpilot.admin.dto.UserInfoResponse
import com.xeno.subpilot.admin.entity.AdminAction
import com.xeno.subpilot.admin.entity.AdminTargetType
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class AdminUserService(
    private val subscriptionClient: SubscriptionAdminClient,
    private val loyaltyClient: LoyaltyAdminClient,
    private val auditService: AuditService,
    private val objectMapper: ObjectMapper,
) {

    suspend fun getUserInfo(userId: Long): UserInfoResponse {
        val info = subscriptionClient.getUserInfo(userId)
        val balance = subscriptionClient.getBalance(userId)
        val loyaltyPoints = loyaltyClient.getBalance(userId)

        val freeRequests = balance.freeBalancesList.sumOf { it.requestsRemaining }.toLong()
        val paidRequests = balance.paidBalancesList.sumOf { it.requestsRemaining }.toLong()

        return UserInfoResponse(
            userId = userId,
            blocked = info.blocked,
            role = info.role,
            registeredAt =
                Instant
                    .ofEpochSecond(info.registeredAtEpoch)
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            loyaltyPoints = loyaltyPoints,
            freeRequestsRemaining = freeRequests,
            paidRequestsRemaining = paidRequests,
        )
    }

    suspend fun banUser(
        operator: String,
        userId: Long,
    ) {
        subscriptionClient.blockUser(userId)
        auditService.record(
            operator = operator,
            action = AdminAction.BAN_USER,
            targetType = AdminTargetType.USER,
            targetId = userId.toString(),
        )
    }

    suspend fun unbanUser(
        operator: String,
        userId: Long,
    ) {
        subscriptionClient.unblockUser(userId)
        auditService.record(
            operator = operator,
            action = AdminAction.UNBAN_USER,
            targetType = AdminTargetType.USER,
            targetId = userId.toString(),
        )
    }

    suspend fun addSubscription(
        operator: String,
        userId: Long,
        request: AddSubscriptionRequest,
    ) {
        subscriptionClient.activateSubscription(
            userId = userId,
            planId = request.planId,
            idempotencyKey = request.idempotencyKey,
        )
        auditService.record(
            operator = operator,
            action = AdminAction.ADD_SUBSCRIPTION,
            targetType = AdminTargetType.USER,
            targetId = userId.toString(),
            payload = objectMapper.writeValueAsString(request),
        )
    }

    suspend fun adjustLoyalty(
        operator: String,
        userId: Long,
        request: AdjustLoyaltyRequest,
    ) {
        loyaltyClient.adjustPoints(
            userId = userId,
            delta = request.delta,
            reason = request.reason,
            idempotencyKey = request.idempotencyKey,
        )
        auditService.record(
            operator = operator,
            action = AdminAction.ADJUST_LOYALTY,
            targetType = AdminTargetType.USER,
            targetId = userId.toString(),
            payload = objectMapper.writeValueAsString(request),
        )
    }
}
