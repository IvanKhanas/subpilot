package com.xeno.subpilot.tgbot.message

import com.xeno.subpilot.proto.chat.v1.ProcessMessageResponse
import com.xeno.subpilot.proto.subscription.v1.DenialReason
import com.xeno.subpilot.tgbot.client.ChatClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.exception.ChatServiceException
import com.xeno.subpilot.tgbot.util.AIResponseWaitingIndicator
import com.xeno.subpilot.tgbot.util.TelegramOpenAIMarkdownConverter
import com.xeno.subpilot.tgbot.ux.AiProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val logger = KotlinLogging.logger {}

@Component
class DefaultMessageHandler(
    private val telegramClient: TelegramClient,
    private val chatClient: ChatClient,
    private val waitingIndicator: AIResponseWaitingIndicator,
) : MessageHandler {

    override fun handle(message: Message) {
        val userId = message.from?.id ?: return
        val chatId = message.chat.id
        val text = message.text ?: return

        logger.atDebug {
            this.message = "telegram_message_received"
            payload = mapOf("user_id" to userId, "chat_id" to chatId, "text" to text)
        }

        val response = callChatService(userId, chatId, text) ?: return

        if (response.denialReason != DenialReason.DENIAL_REASON_UNSPECIFIED) {
            sendDenialResponse(chatId, userId, response)
            return
        }

        sendSuccessResponse(chatId, response)
    }

    private fun callChatService(
        userId: Long,
        chatId: Long,
        text: String,
    ): ProcessMessageResponse? =
        try {
            waitingIndicator.wrap(chatId) {
                chatClient.processMessage(userId, chatId, text)
            }
        } catch (ex: ChatServiceException) {
            logger.atError {
                this.message = "chat_service_unavailable"
                cause = ex
                payload = mapOf("user_id" to userId, "chat_id" to chatId)
            }
            telegramClient.sendMessage(chatId, BotResponses.AI_UNAVAILABLE_RESPONSE.text)
            null
        }

    private fun sendDenialResponse(
        chatId: Long,
        userId: Long,
        response: ProcessMessageResponse,
    ) {
        logger.atWarn {
            this.message = "access_denied"
            payload =
                mapOf("user_id" to userId, "chat_id" to chatId, "reason" to response.denialReason)
        }
        telegramClient.sendMessage(
            chatId,
            denialText(
                response.denialReason,
                response.modelId,
                response.availableRequests,
                response.modelCost,
            ),
        )
    }

    private fun sendSuccessResponse(
        chatId: Long,
        response: ProcessMessageResponse,
    ) {
        telegramClient.sendMessage(
            chatId,
            TelegramOpenAIMarkdownConverter.toHtml(response.text),
            parseMode = "HTML",
        )
        if (response.resetAtEpoch != 0L) {
            telegramClient.sendMessage(
                chatId,
                freeQuotaExhaustedText(response.modelId, response.resetAtEpoch),
            )
        }
    }

    private fun freeQuotaExhaustedText(
        modelId: String,
        resetAtEpoch: Long,
    ): String {
        val providerName = AiProvider.findProviderByModelId(modelId)?.displayName ?: modelId
        val resetAt =
            Instant
                .ofEpochSecond(resetAtEpoch)
                .atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm 'UTC'", Locale.ENGLISH))
        return BotResponses.FREE_QUOTA_EXHAUSTED_RESPONSE.format(providerName, resetAt)
    }

    private fun denialText(
        reason: DenialReason,
        modelId: String,
        availableRequests: Int,
        modelCost: Int,
    ): String =
        when (reason) {
            DenialReason.QUOTA_EXHAUSTED -> BotResponses.QUOTA_EXCEEDED_RESPONSE.text
            DenialReason.NO_SUBSCRIPTION -> {
                val providerName = AiProvider.findProviderByModelId(modelId)?.displayName ?: modelId
                val modelName = AiProvider.findModelById(modelId)?.displayName ?: modelId
                BotResponses.NO_SUBSCRIPTION_RESPONSE.format(
                    availableRequests,
                    providerName,
                    modelName,
                    modelCost,
                )
            }
            DenialReason.BLOCKED -> BotResponses.ACCESS_BLOCKED_RESPONSE.text
            else -> BotResponses.AI_UNAVAILABLE_RESPONSE.text
        }
}
