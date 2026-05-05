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

import com.xeno.subpilot.admin.client.SubscriptionAdminClient
import com.xeno.subpilot.admin.dto.CreatePlanRequest
import com.xeno.subpilot.admin.entity.AdminAction
import com.xeno.subpilot.admin.entity.AdminTargetType
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class AdminSubscriptionService(
    private val subscriptionClient: SubscriptionAdminClient,
    private val auditService: AuditService,
    private val objectMapper: ObjectMapper,
) {

    suspend fun createPlan(
        operator: String,
        request: CreatePlanRequest,
    ) {
        subscriptionClient.createPlan(
            planId = request.planId,
            provider = request.provider,
            displayName = request.displayName,
            price = request.price.toPlainString(),
            currency = request.currency,
            allocations = request.allocations.map { it.provider to it.requests },
        )
        auditService.record(
            operator = operator,
            action = AdminAction.CREATE_PLAN,
            targetType = AdminTargetType.PLAN,
            targetId = request.planId,
            payload = objectMapper.writeValueAsString(request),
        )
    }
}
