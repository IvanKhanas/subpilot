package com.xeno.subpilot.tgbot.client

import com.xeno.subpilot.tgbot.dto.AnswerCallbackQueryRequest
import com.xeno.subpilot.tgbot.dto.BotCommandInfo
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
        } catch (e: Exception) {
            logger.error(e) { "Failed to get updates" }
            emptyList()
        }
    }

    override fun sendMessage(
        chatId: Long,
        text: String,
        replyMarkup: ReplyMarkup?,
    ) {
        try {
            telegramRestClient
                .post()
                .uri("/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(SendMessageRequest(chatId = chatId, text = text, replyMarkup = replyMarkup))
                .retrieve()
                .toBodilessEntity()
        } catch (e: Exception) {
            logger.error(e) { "Failed to send message to chat $chatId" }
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
        } catch (e: Exception) {
            logger.error(e) { "Failed to answer callback query $callbackQueryId" }
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
        } catch (e: Exception) {
            logger.error(e) { "Failed to set bot commands" }
        }
    }
}
