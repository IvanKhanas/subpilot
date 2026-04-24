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

import com.xeno.subpilot.admin.exception.UserNotFoundException
import com.xeno.subpilot.proto.subscription.v1.GetBalanceResponse
import com.xeno.subpilot.proto.subscription.v1.GetUserInfoResponse
import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import com.xeno.subpilot.proto.subscription.v1.blockUserRequest
import com.xeno.subpilot.proto.subscription.v1.getBalanceRequest
import com.xeno.subpilot.proto.subscription.v1.getUserInfoRequest
import com.xeno.subpilot.proto.subscription.v1.unblockUserRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.StatusException
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class SubscriptionAdminGrpcClient(
    private val stub: SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineStub,
) {

    suspend fun getUserInfo(userId: Long): GetUserInfoResponse {
        val response = stub.getUserInfo(getUserInfoRequest { this.userId = userId })
        if (!response.found) throw UserNotFoundException(userId)
        return response
    }

    suspend fun getBalance(userId: Long): GetBalanceResponse =
        stub.getBalance(getBalanceRequest { this.userId = userId })

    suspend fun blockUser(userId: Long) {
        try {
            stub.blockUser(blockUserRequest { this.userId = userId })
        } catch (ex: StatusException) {
            logger.atError {
                this.message = "admin_block_user_failed"
                cause = ex
                payload = mapOf("user_id" to userId)
            }
            throw ex
        }
    }

    suspend fun unblockUser(userId: Long) {
        try {
            stub.unblockUser(unblockUserRequest { this.userId = userId })
        } catch (ex: StatusException) {
            logger.atError {
                this.message = "admin_unblock_user_failed"
                cause = ex
                payload = mapOf("user_id" to userId)
            }
            throw ex
        }
    }
}