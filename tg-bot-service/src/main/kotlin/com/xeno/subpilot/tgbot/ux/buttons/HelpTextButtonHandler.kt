package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.CommandResponses
import com.xeno.subpilot.tgbot.dto.Message
import org.springframework.stereotype.Component

@Component
class HelpTextButtonHandler(
    private val telegramClient: TelegramClient,
) : TextButtonHandler {

    override fun supports(text: String): Boolean = text == BotButtons.BTN_HELP

    override fun handle(message: Message) {
        telegramClient.sendMessage(
            message.chat.id,
            CommandResponses.HELP_RESPONSE.text,
        )
    }
}
