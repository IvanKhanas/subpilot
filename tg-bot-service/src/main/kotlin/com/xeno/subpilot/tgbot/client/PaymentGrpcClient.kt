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
package com.xeno.subpilot.tgbot.client

import com.xeno.subpilot.proto.payment.v1.PaymentServiceGrpcKt
import com.xeno.subpilot.proto.payment.v1.createPaymentRequest
import com.xeno.subpilot.tgbot.exception.PaymentServiceException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.StatusException
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class PaymentGrpcClient(
    private val stub: PaymentServiceGrpcKt.PaymentServiceCoroutineStub,
    private val grpcRetry: GrpcRetry,
) : PaymentClient {

    override suspend fun createPayment(
        userId: Long,
        planId: String,
        bonusPointsToApply: Long,
    ): String {
        try {
            return grpcRetry.retryOnUnavailable {
                stub
                    .createPayment(
                        createPaymentRequest {
                            this.userId = userId
                            this.planId = planId
                            this.bonusPointsToApply = bonusPointsToApply
                        },
                    ).confirmationUrl
            }
        } catch (ex: StatusException) {
            logger.atError {
                message = "payment_create_payment_failed"
                cause = ex
                payload = mapOf("user_id" to userId, "plan_id" to planId)
            }
            throw PaymentServiceException("Failed to create payment", ex)
        }
    }
}
