package com.xeno.subpilot.tgbot.command

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.buttons.BotButtons
import org.springframework.stereotype.Component

@Component
class StartCommandHandler(
    private val telegramClient: TelegramClient,
) : BotCommand {

    override val command = "/start"
    override val description = "Start the bot"

    override fun handle(message: Message) {
        val userName = message.from?.firstName ?: DEFAULT_USERNAME

        telegramClient.sendMessage(
            chatId = message.chat.id,
            text = CommandResponses.START_RESPONSE.format(userName),
            replyMarkup = BotButtons.mainMenu,
        )
    }

    companion object {
        internal const val DEFAULT_USERNAME = "friend"
    }
}
