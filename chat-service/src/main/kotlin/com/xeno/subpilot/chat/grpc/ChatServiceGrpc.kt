package com.xeno.subpilot.chat.grpc

import com.xeno.subpilot.chat.client.OpenAiChatClient
import com.xeno.subpilot.chat.client.SubscriptionGrpcClient
import com.xeno.subpilot.chat.service.ChatHistoryService
import com.xeno.subpilot.proto.chat.v1.ChatServiceGrpcKt
import com.xeno.subpilot.proto.chat.v1.ClearHistoryRequest
import com.xeno.subpilot.proto.chat.v1.ClearHistoryResponse
import com.xeno.subpilot.proto.chat.v1.ProcessMessageRequest
import com.xeno.subpilot.proto.chat.v1.ProcessMessageResponse
import com.xeno.subpilot.proto.chat.v1.clearHistoryResponse
import com.xeno.subpilot.proto.chat.v1.processMessageResponse
import com.xeno.subpilot.proto.subscription.v1.CheckAccessResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.grpc.server.service.GrpcService

import kotlin.coroutines.CoroutineContext

import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

@GrpcService
class ChatServiceGrpc(
    private val openAiChatClient: OpenAiChatClient,
    private val chatHistoryService: ChatHistoryService,
    private val subscriptionGrpcClient: SubscriptionGrpcClient,
    private val ioDispatcher: CoroutineContext,
) : ChatServiceGrpcKt.ChatServiceCoroutineImplBase() {

    override suspend fun processMessage(request: ProcessMessageRequest): ProcessMessageResponse {
        logger.atDebug {
            message = "grpc_process_message"
            payload = mapOf("user_id" to request.userId, "chat_id" to request.chatId)
        }

        val model = subscriptionGrpcClient.getModelPreference(request.userId)
        val access = subscriptionGrpcClient.checkAccess(request.userId, model)

        if (!access.allowed) {
            return buildDenialResponse(access, model)
        }

        return generateAndSaveAiResponse(request, model, access)
    }

    private fun buildDenialResponse(
        access: CheckAccessResponse,
        model: String,
    ): ProcessMessageResponse =
        processMessageResponse {
            denialReason = access.denialReason
            modelId = model
            availableRequests = access.availableRequests
            modelCost = access.modelCost
        }

    private suspend fun generateAndSaveAiResponse(
        request: ProcessMessageRequest,
        model: String,
        access: CheckAccessResponse,
    ): ProcessMessageResponse {
        try {
            val history =
                withContext(ioDispatcher) {
                    chatHistoryService.getHistory(request.chatId)
                }

            val aiText = openAiChatClient.chat(history, request.text, model)

            withContext(ioDispatcher) {
                chatHistoryService.append(request.chatId, request.text, aiText)
            }

            return processMessageResponse {
                text = aiText
                resetAtEpoch = access.resetAtEpoch
                modelId = model
            }
        } catch (ex: Exception) {
            subscriptionGrpcClient.refundAccess(
                request.userId,
                model,
                access.freeConsumed,
                access.paidConsumed,
            )
            throw ex
        }
    }

    override suspend fun clearHistory(request: ClearHistoryRequest): ClearHistoryResponse {
        logger.atDebug {
            message = "grpc_clear_history"
            payload = mapOf("chat_id" to request.chatId)
        }
        withContext(ioDispatcher) {
            chatHistoryService.clear(request.chatId)
        }
        return clearHistoryResponse { }
    }
}
