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
package com.xeno.subpilot.chat.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Status
import io.grpc.StatusException
import org.springframework.grpc.server.exception.GrpcExceptionHandler
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class ChatGrpcExceptionHandler : GrpcExceptionHandler {

    override fun handleException(e: Throwable): StatusException =
        when (e) {
            is ChatException -> {
                logger.atError {
                    message = "grpc_chat_exception"
                    cause = e
                }
                e.status
                    .withDescription(e.message)
                    .withCause(e)
                    .asException()
            }
            else -> {
                logger.atError {
                    message = "grpc_unhandled_exception"
                    cause = e
                }
                Status.INTERNAL
                    .withDescription(e.message)
                    .withCause(e)
                    .asException()
            }
        }
}
