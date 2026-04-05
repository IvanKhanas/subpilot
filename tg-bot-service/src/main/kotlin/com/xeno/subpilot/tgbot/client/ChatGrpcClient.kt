package com.xeno.subpilot.tgbot.client

import com.xeno.subpilot.proto.chat.v1.ChatServiceGrpcKt
import com.xeno.subpilot.proto.chat.v1.processMessageRequest
import com.xeno.subpilot.proto.chat.v1.setModelRequest
import com.xeno.subpilot.tgbot.exception.ChatServiceException
import io.grpc.StatusException
import org.springframework.stereotype.Component

import kotlinx.coroutines.runBlocking

@Component
class ChatGrpcClient(
    private val stub: ChatServiceGrpcKt.ChatServiceCoroutineStub,
) : ChatClient {

    override fun processMessage(
        userId: Long,
        chatId: Long,
        text: String,
    ): String =
        runBlocking {
            try {
                stub
                    .processMessage(
                        processMessageRequest {
                            this.userId = userId
                            this.chatId = chatId
                            this.text = text
                        },
                    ).text
            } catch (ex: StatusException) {
                throw ChatServiceException("Chat service call failed: ${ex.status}", ex)
            }
        }

    override fun setModel(
        chatId: Long,
        model: String,
    ): Unit =
        runBlocking {
            try {
                stub.setModel(
                    setModelRequest {
                        this.chatId = chatId
                        this.model = model
                    },
                )
            } catch (ex: StatusException) {
                throw ChatServiceException("Chat service call failed: ${ex.status}", ex)
            }
        }
}
