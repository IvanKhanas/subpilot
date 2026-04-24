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

import com.xeno.subpilot.chat.config.GrpcRetryProperties
import io.grpc.Status
import io.grpc.StatusException
import org.springframework.stereotype.Component

import kotlinx.coroutines.delay

@Component
class GrpcRetry(
    private val props: GrpcRetryProperties,
) {

    suspend fun <T> retryOnUnavailable(block: suspend () -> T): T {
        var backoffMs = props.initialBackoffMs
        repeat(props.maxAttempts - 1) {
            try {
                return block()
            } catch (ex: StatusException) {
                if (ex.status.code != Status.Code.UNAVAILABLE) throw ex
            }
            delay(backoffMs)
            backoffMs = (backoffMs * props.backoffMultiplier).toLong()
        }
        return block()
    }
}
