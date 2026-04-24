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

import com.xeno.subpilot.proto.chat.v1.ChatServiceGrpcKt
import com.xeno.subpilot.proto.chat.v1.ProcessMessageResponse
import com.xeno.subpilot.proto.chat.v1.clearContextRequest
import com.xeno.subpilot.proto.chat.v1.processMessageRequest
import com.xeno.subpilot.tgbot.exception.ChatServiceException
import io.grpc.StatusException
import org.springframework.stereotype.Component

@Component
class ChatGrpcClient(
    private val stub: ChatServiceGrpcKt.ChatServiceCoroutineStub,
    private val grpcRetry: GrpcRetry,
) : ChatClient {

    override suspend fun processMessage(
        userId: Long,
        chatId: Long,
        text: String,
    ): ProcessMessageResponse =
        try {
            stub.processMessage(
                processMessageRequest {
                    this.userId = userId
                    this.chatId = chatId
                    this.text = text
                },
            )
        } catch (ex: StatusException) {
            throw ChatServiceException("Chat service call failed: ${ex.status}", ex)
        }

    override suspend fun clearContext(chatId: Long) {
        try {
            grpcRetry.retryOnUnavailable {
                stub.clearHistory(clearContextRequest { this.chatId = chatId })
            }
        } catch (ex: StatusException) {
            throw ChatServiceException("Chat service call failed: ${ex.status}", ex)
        }
    }
}
