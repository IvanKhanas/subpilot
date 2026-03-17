package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.CommandResponses
import com.xeno.subpilot.tgbot.dto.CallbackQuery
import com.xeno.subpilot.tgbot.message.CallbackHandler
import org.springframework.stereotype.Component

@Component(BotButtons.START_CHAT)
class StartChatCallbackHandler(
    private val telegramClient: TelegramClient,
) : CallbackHandler {

    override fun handle(callbackQuery: CallbackQuery) {
        val chatId = callbackQuery.message?.chat?.id ?: return
        telegramClient.sendMessage(chatId, CommandResponses.HELP_RESPONSE.text)
    }
}
