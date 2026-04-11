package com.xeno.subpilot.chat.grpc

import com.xeno.subpilot.chat.client.OpenAiChatClient
import com.xeno.subpilot.chat.client.SubscriptionGrpcClient
import com.xeno.subpilot.chat.exception.OpenAiException
import com.xeno.subpilot.chat.properties.OpenAiProperties
import com.xeno.subpilot.chat.service.ChatHistoryService
import com.xeno.subpilot.proto.chat.v1.ChatServiceGrpcKt
import com.xeno.subpilot.proto.chat.v1.ProcessMessageRequest
import com.xeno.subpilot.proto.chat.v1.ProcessMessageResponse
import com.xeno.subpilot.proto.chat.v1.processMessageResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.grpc.server.service.GrpcService

private val logger = KotlinLogging.logger {}

@GrpcService
class ChatServiceGrpc(
    private val openAiChatClient: OpenAiChatClient,
    private val chatHistoryService: ChatHistoryService,
    private val subscriptionGrpcClient: SubscriptionGrpcClient,
    private val openAiProperties: OpenAiProperties,
) : ChatServiceGrpcKt.ChatServiceCoroutineImplBase() {

    override suspend fun processMessage(request: ProcessMessageRequest): ProcessMessageResponse {
        logger.atDebug {
            message = "grpc_process_message"
            payload = mapOf("user_id" to request.userId, "chat_id" to request.chatId)
        }

        val model = subscriptionGrpcClient.getModelPreference(request.userId)
            .ifBlank { openAiProperties.defaultModel }

        val access = subscriptionGrpcClient.checkAccess(request.userId, model)
        if (!access.allowed) {
            return processMessageResponse { denialReason = access.denialReason }
        }

        val history = withContext(Dispatchers.IO) {
            chatHistoryService.getHistory(request.chatId)
        }

        val aiText = try {
            openAiChatClient.chat(history, request.text, model)
        } catch (ex: OpenAiException) {
            subscriptionGrpcClient.refundAccess(request.userId, model)
            throw ex
        }

        withContext(Dispatchers.IO) {
            chatHistoryService.append(request.chatId, request.text, aiText)
        }

        return processMessageResponse { text = aiText }
    }
}
