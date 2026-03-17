package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import org.springframework.stereotype.Component

@Component
class ChatTextButtonHandler(
    private val telegramClient: TelegramClient,
) : TextButtonHandler {
    override fun supports(text: String): Boolean = text == BotButtons.BTN_CHAT

    override fun handle(message: Message) {
        telegramClient.sendMessage(message.chat.id, DEFAULT_MESSAGE)
    }

    companion object {
        internal const val DEFAULT_MESSAGE = "Send me any message and I'll reply!"
    }
}
