package com.xeno.subpilot.tgbot.ux

import com.xeno.subpilot.tgbot.client.TelegramClient
import org.springframework.stereotype.Component

@Component
class ScreenRenderer(
    private val telegramClient: TelegramClient,
) {
    fun render(
        chatId: Long,
        screen: BotScreen,
    ) {
        telegramClient.sendMessage(chatId, screen.responseText, screen.replyMarkup)
    }
}
