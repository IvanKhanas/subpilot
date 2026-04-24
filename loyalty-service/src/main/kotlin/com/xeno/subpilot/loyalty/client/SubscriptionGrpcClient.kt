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
package com.xeno.subpilot.loyalty.client

import com.xeno.subpilot.loyalty.exception.SubscriptionServiceException
import com.xeno.subpilot.proto.subscription.v1.PlanInfo
import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import com.xeno.subpilot.proto.subscription.v1.activateSubscriptionRequest
import com.xeno.subpilot.proto.subscription.v1.getPlanInfoRequest
import io.grpc.Status
import io.grpc.StatusException
import org.springframework.stereotype.Component

import java.util.UUID

import kotlinx.coroutines.runBlocking

@Component
class SubscriptionGrpcClient(
    private val stub: SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineStub,
    private val grpcRetry: GrpcRetry,
) {

    fun getPlanInfo(planId: String): PlanInfo? =
        try {
            runBlocking {
                stub.getPlanInfo(getPlanInfoRequest { this.planId = planId }).plan
            }
        } catch (ex: StatusException) {
            if (ex.status.code == Status.Code.NOT_FOUND) {
                null
            } else {
                throw SubscriptionServiceException(
                    "Subscription service getPlanInfo failed: ${ex.status}",
                    ex,
                )
            }
        }

    fun activateSubscription(
        userId: Long,
        planId: String,
        idempotencyKey: UUID,
    ) {
        try {
            runBlocking {
                grpcRetry.retryOnUnavailable {
                    stub.activateSubscription(
                        activateSubscriptionRequest {
                            this.userId = userId
                            this.planId = planId
                            this.idempotencyKey = idempotencyKey.toString()
                        },
                    )
                }
            }
        } catch (ex: StatusException) {
            throw SubscriptionServiceException(
                "Subscription service activateSubscription failed: ${ex.status}",
                ex,
            )
        }
    }
}
