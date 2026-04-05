package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.message.BotResponses
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(2)
class ChatTextButtonHandler(
    private val telegramClient: TelegramClient,
) : TextButtonHandler {

    override fun supports(text: String) = text == BotButtons.BTN_START_CHAT

    override fun handle(message: Message) {
        telegramClient.sendMessage(message.chat.id, BotResponses.CHAT_PROMPT_RESPONSE.text)
    }
}
