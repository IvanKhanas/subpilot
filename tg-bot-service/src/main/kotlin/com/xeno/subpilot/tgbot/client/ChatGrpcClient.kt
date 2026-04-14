package com.xeno.subpilot.tgbot.client

import com.xeno.subpilot.proto.chat.v1.ChatServiceGrpcKt
import com.xeno.subpilot.proto.chat.v1.ProcessMessageResponse
import com.xeno.subpilot.proto.chat.v1.clearHistoryRequest
import com.xeno.subpilot.proto.chat.v1.processMessageRequest
import com.xeno.subpilot.tgbot.exception.ChatServiceException
import io.grpc.StatusException
import org.springframework.stereotype.Component

import kotlinx.coroutines.runBlocking

@Component
class ChatGrpcClient(
    private val stub: ChatServiceGrpcKt.ChatServiceCoroutineStub,
    private val grpcRetry: GrpcRetry,
) : ChatClient {

    override fun processMessage(
        userId: Long,
        chatId: Long,
        text: String,
    ): ProcessMessageResponse =
        runBlocking {
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
        }

    override fun clearHistory(chatId: Long) {
        runBlocking {
            try {
                grpcRetry.retryOnUnavailable {
                    stub.clearHistory(clearHistoryRequest { this.chatId = chatId })
                }
            } catch (ex: StatusException) {
                throw ChatServiceException("Chat service call failed: ${ex.status}", ex)
            }
        }
    }
}
