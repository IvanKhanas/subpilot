package com.xeno.subpilot.tgbot.command

import com.xeno.subpilot.tgbot.client.ChatClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.message.BotResponses
import org.springframework.stereotype.Component

@Component
class ClearCommandHandler(
    private val chatClient: ChatClient,
    private val telegramClient: TelegramClient,
) : BotCommand {

    override val command = "/clear"
    override val description = "Clear context for current model"

    override suspend fun handle(message: Message) {
        chatClient.clearContext(message.chat.id)
        telegramClient.sendMessage(message.chat.id, BotResponses.CONTEXT_CLEARED_RESPONSE.text)
    }
}
