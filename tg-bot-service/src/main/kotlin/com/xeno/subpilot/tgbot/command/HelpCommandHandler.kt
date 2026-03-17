package com.xeno.subpilot.tgbot.command

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import org.springframework.stereotype.Component

@Component
class HelpCommandHandler(
    private val telegramClient: TelegramClient,
) : BotCommand {

    override val command = "/help"
    override val description = "Show available commands"

    override fun handle(message: Message) {
        telegramClient.sendMessage(
            chatId = message.chat.id,
            text = CommandResponses.HELP_RESPONSE.text,
        )
    }
}
