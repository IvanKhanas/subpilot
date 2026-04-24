package com.xeno.subpilot.tgbot.client

import com.xeno.subpilot.tgbot.dto.AnswerCallbackQueryRequest
import com.xeno.subpilot.tgbot.dto.BotCommandInfo
import com.xeno.subpilot.tgbot.dto.DeleteMessageRequest
import com.xeno.subpilot.tgbot.dto.EditMessageTextRequest
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.ReplyMarkup
import com.xeno.subpilot.tgbot.dto.SendMessageRequest
import com.xeno.subpilot.tgbot.dto.SetMyCommandsRequest
import com.xeno.subpilot.tgbot.dto.TelegramResponse
import com.xeno.subpilot.tgbot.dto.Update
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

import java.net.http.HttpTimeoutException

private val logger = KotlinLogging.logger {}

@Component
class RestTelegramClient(
    private val telegramRestClient: RestClient,
) : TelegramClient {

    override fun getUpdates(
        offset: Long?,
        timeout: Int,
    ): List<Update> {
        val params =
            buildMap {
                if (offset != null) put("offset", offset)
                put("timeout", timeout)
                put("allowed_updates", listOf("message", "callback_query"))
            }

        return try {
            val response =
                telegramRestClient
                    .post()
                    .uri("/getUpdates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(params)
                    .retrieve()
                    .body(object : ParameterizedTypeReference<TelegramResponse<List<Update>>>() {})

            if (response?.ok == true) response.result.orEmpty() else emptyList()
        } catch (ex: RestClientException) {
            if (ex.cause is HttpTimeoutException) return emptyList()
            logger.atError {
                message = "telegram_get_updates_failed"
                cause = ex
            }
            emptyList()
        }
    }

    override fun sendMessage(
        chatId: Long,
        text: String,
        replyMarkup: ReplyMarkup?,
        parseMode: String?,
    ): Long? =
        try {
            val response =
                telegramRestClient
                    .post()
                    .uri("/sendMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        SendMessageRequest(
                            chatId = chatId,
                            text = text,
                            replyMarkup = replyMarkup,
                            parseMode = parseMode,
                        ),
                    ).retrieve()
                    .body(object : ParameterizedTypeReference<TelegramResponse<Message>>() {})
            response?.result?.messageId
        } catch (ex: RestClientException) {
            logger.atError {
                message = "telegram_send_message_failed"
                cause = ex
                payload = mapOf("chat_id" to chatId)
            }
            null
        }

    override fun editMessage(
        chatId: Long,
        messageId: Long,
        text: String,
    ) {
        try {
            telegramRestClient
                .post()
                .uri("/editMessageText")
                .contentType(MediaType.APPLICATION_JSON)
                .body(EditMessageTextRequest(chatId = chatId, messageId = messageId, text = text))
                .retrieve()
                .toBodilessEntity()
        } catch (ex: RestClientException) {
            logger.atError {
                message = "telegram_edit_message_failed"
                cause = ex
                payload = mapOf("chat_id" to chatId, "message_id" to messageId)
            }
        }
    }

    override fun deleteMessage(
        chatId: Long,
        messageId: Long,
    ) {
        try {
            telegramRestClient
                .post()
                .uri("/deleteMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    DeleteMessageRequest(chatId = chatId, messageId = messageId),
                ).retrieve()
                .toBodilessEntity()
        } catch (ex: RestClientException) {
            logger.atError {
                message = "telegram_delete_message_failed"
                cause = ex
                payload = mapOf("chat_id" to chatId, "message_id" to messageId)
            }
        }
    }

    override fun answerCallbackQuery(callbackQueryId: String) {
        try {
            telegramRestClient
                .post()
                .uri("/answerCallbackQuery")
                .contentType(MediaType.APPLICATION_JSON)
                .body(AnswerCallbackQueryRequest(callbackQueryId = callbackQueryId))
                .retrieve()
                .toBodilessEntity()
        } catch (ex: RestClientException) {
            logger.atError {
                message = "telegram_answer_callback_failed"
                cause = ex
                payload = mapOf("callback_query_id" to callbackQueryId)
            }
        }
    }

    override fun setMyCommands(commands: List<BotCommandInfo>) {
        try {
            telegramRestClient
                .post()
                .uri("/setMyCommands")
                .contentType(MediaType.APPLICATION_JSON)
                .body(SetMyCommandsRequest(commands = commands))
                .retrieve()
                .toBodilessEntity()
        } catch (ex: RestClientException) {
            logger.atError {
                message = "telegram_set_commands_failed"
                cause = ex
            }
        }
    }
}
