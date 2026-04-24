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
package com.xeno.subpilot.chat.client

import com.xeno.subpilot.chat.exception.SubscriptionServiceException
import com.xeno.subpilot.proto.subscription.v1.CheckAccessResponse
import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import com.xeno.subpilot.proto.subscription.v1.checkAccessRequest
import com.xeno.subpilot.proto.subscription.v1.getModelPreferenceRequest
import com.xeno.subpilot.proto.subscription.v1.refundAccessRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.StatusException
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class SubscriptionGrpcClient(
    private val stub: SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineStub,
    private val grpcRetry: GrpcRetry,
) {
    suspend fun checkAccess(
        userId: Long,
        modelId: String,
    ): CheckAccessResponse =
        try {
            stub.checkAccess(
                checkAccessRequest {
                    this.userId = userId
                    this.modelId = modelId
                },
            )
        } catch (ex: StatusException) {
            throw SubscriptionServiceException("Subscription service call failed: ${ex.status}", ex)
        }

    suspend fun getModelPreference(userId: Long): String =
        try {
            grpcRetry.retryOnUnavailable {
                stub.getModelPreference(getModelPreferenceRequest { this.userId = userId }).modelId
            }
        } catch (ex: StatusException) {
            throw SubscriptionServiceException("Subscription service call failed: ${ex.status}", ex)
        }

    suspend fun refundAccess(
        userId: Long,
        modelId: String,
        freeConsumed: Int,
        paidConsumed: Int,
    ) {
        try {
            stub.refundAccess(
                refundAccessRequest {
                    this.userId = userId
                    this.modelId = modelId
                    this.freeConsumed = freeConsumed
                    this.paidConsumed = paidConsumed
                },
            )
        } catch (ex: StatusException) {
            logger.atWarn {
                message = "subscription_refund_access_failed"
                cause = ex
                payload = mapOf("user_id" to userId, "model_id" to modelId)
            }
        }
    }
}
