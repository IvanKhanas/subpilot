package com.xeno.subpilot.chat.grpc

import com.xeno.subpilot.chat.client.OpenAiChatClient
import com.xeno.subpilot.chat.repository.ChatModelPreferenceRepository
import com.xeno.subpilot.chat.service.ChatHistoryService
import com.xeno.subpilot.proto.chat.v1.ChatServiceGrpcKt
import com.xeno.subpilot.proto.chat.v1.ProcessMessageRequest
import com.xeno.subpilot.proto.chat.v1.ProcessMessageResponse
import com.xeno.subpilot.proto.chat.v1.SetModelRequest
import com.xeno.subpilot.proto.chat.v1.SetModelResponse
import com.xeno.subpilot.proto.chat.v1.processMessageResponse
import com.xeno.subpilot.proto.chat.v1.setModelResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.grpc.server.service.GrpcService

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

@GrpcService
class ChatServiceGrpcImpl(
    private val chatModelPreferenceRepository: ChatModelPreferenceRepository,
    private val openAiChatClient: OpenAiChatClient,
    private val chatHistoryService: ChatHistoryService,
) : ChatServiceGrpcKt.ChatServiceCoroutineImplBase() {

    override suspend fun processMessage(request: ProcessMessageRequest): ProcessMessageResponse {
        logger.atDebug {
            message = "grpc_process_message"
            payload = mapOf("user_id" to request.userId, "chat_id" to request.chatId)
        }

        val (model, history) = loadContext(request.chatId)
        val aiText = openAiChatClient.chat(history, request.text, model)
        saveHistory(request.chatId, request.text, aiText)

        return processMessageResponse { text = aiText }
    }

    private suspend fun loadContext(chatId: Long) =
        withContext(Dispatchers.IO) {
            chatModelPreferenceRepository.getModel(chatId) to chatHistoryService.getHistory(chatId)
        }

    private suspend fun saveHistory(
        chatId: Long,
        userText: String,
        aiText: String,
    ) = withContext(Dispatchers.IO) {
        chatHistoryService.append(chatId, userText, aiText)
    }

    override suspend fun setModel(request: SetModelRequest): SetModelResponse {
        logger.atDebug {
            message = "grpc_set_model"
            payload = mapOf("model" to request.model, "chat_id" to request.chatId)
        }
        withContext(Dispatchers.IO) {
            chatModelPreferenceRepository.setModel(request.chatId, request.model)
        }
        return setModelResponse { }
    }
}
