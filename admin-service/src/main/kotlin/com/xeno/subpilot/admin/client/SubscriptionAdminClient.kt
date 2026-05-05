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
package com.xeno.subpilot.admin.client

import com.xeno.subpilot.proto.subscription.v1.GetBalanceResponse
import com.xeno.subpilot.proto.subscription.v1.GetUserInfoResponse

interface SubscriptionAdminClient {
    suspend fun getUserInfo(userId: Long): GetUserInfoResponse

    suspend fun getBalance(userId: Long): GetBalanceResponse

    suspend fun blockUser(userId: Long)

    suspend fun unblockUser(userId: Long)

    suspend fun activateSubscription(
        userId: Long,
        planId: String,
        idempotencyKey: String,
    )

    suspend fun createPlan(
        planId: String,
        provider: String,
        displayName: String,
        price: String,
        currency: String,
        allocations: List<Pair<String, Int>>,
    )
}
