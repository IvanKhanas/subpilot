package com.xeno.subpilot.tgbot.message

import com.xeno.subpilot.tgbot.client.ChatClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.exception.ChatServiceException
import com.xeno.subpilot.tgbot.util.AIResponseWaitingIndicator
import com.xeno.subpilot.tgbot.util.TelegramOpenAIMarkdownConverter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

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

        try {
            val response =
                waitingIndicator.wrap(chatId) {
                    chatClient.processMessage(userId, chatId, text)
                }
            telegramClient.sendMessage(
                chatId,
                TelegramOpenAIMarkdownConverter.toHtml(response),
                parseMode = "HTML",
            )
        } catch (ex: ChatServiceException) {
            logger.atError {
                this.message = "chat_service_unavailable"
                cause = ex
                payload = mapOf("user_id" to userId, "chat_id" to chatId)
            }
            telegramClient.sendMessage(chatId, BotResponses.AI_UNAVAILABLE_RESPONSE.text)
        }
    }
}
