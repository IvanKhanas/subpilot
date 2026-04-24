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
package com.xeno.subpilot.payment.client

import com.xeno.subpilot.payment.dto.PlanDetails
import com.xeno.subpilot.payment.exception.InvalidPlanException
import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import com.xeno.subpilot.proto.subscription.v1.getPlanInfoRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Status
import io.grpc.StatusException
import org.springframework.stereotype.Component

import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

@Component
class SubscriptionGrpcClient(
    private val stub: SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineStub,
) {

    suspend fun getPlanDetails(planId: String): PlanDetails {
        try {
            val response = stub.getPlanInfo(getPlanInfoRequest { this.planId = planId })
            return PlanDetails(
                price = BigDecimal(response.plan.price),
                currency = response.plan.currency,
            )
        } catch (ex: StatusException) {
            if (ex.status.code == Status.Code.NOT_FOUND) throw InvalidPlanException(planId)
            logger.atError {
                message = "subscription_get_plan_info_failed"
                cause = ex
                payload = mapOf("plan_id" to planId)
            }
            throw ex
        }
    }
}
