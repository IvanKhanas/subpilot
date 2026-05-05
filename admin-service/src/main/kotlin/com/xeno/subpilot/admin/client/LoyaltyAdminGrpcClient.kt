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
package com.xeno.subpilot.admin.client

import com.xeno.subpilot.proto.loyalty.v1.LoyaltyServiceGrpcKt
import com.xeno.subpilot.proto.loyalty.v1.adjustPointsRequest
import com.xeno.subpilot.proto.loyalty.v1.getBalanceRequest
import org.springframework.stereotype.Component

@Component
class LoyaltyAdminGrpcClient(
    private val stub: LoyaltyServiceGrpcKt.LoyaltyServiceCoroutineStub,
    private val grpcRetry: GrpcRetry,
) : LoyaltyAdminClient {

    override suspend fun getBalance(userId: Long): Long =
        grpcRetry
            .retryOnUnavailable {
                stub.getBalance(getBalanceRequest { this.userId = userId })
            }.points

    override suspend fun adjustPoints(
        userId: Long,
        delta: Long,
        reason: String,
        idempotencyKey: String,
    ) {
        grpcRetry.retryOnUnavailable {
            stub.adjustPoints(
                adjustPointsRequest {
                    this.userId = userId
                    this.delta = delta
                    this.reason = reason
                    this.idempotencyKey = idempotencyKey
                },
            )
        }
    }
}
