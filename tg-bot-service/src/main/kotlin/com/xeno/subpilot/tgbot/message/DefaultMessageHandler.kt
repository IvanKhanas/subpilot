package com.xeno.subpilot.tgbot.message

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class DefaultMessageHandler(
    private val telegramClient: TelegramClient,
) : MessageHandler {

    override fun handle(message: Message) {
        val userId = message.from?.id
        val chatId = message.chat.id

        logger.debug { "Message from userId=$userId chatId=$chatId text=${message.text}" }

        // TODO: forward to chat-service via gRPC
        telegramClient.sendMessage(chatId, "I got your message. AI integration is still in progress.")
    }
}
