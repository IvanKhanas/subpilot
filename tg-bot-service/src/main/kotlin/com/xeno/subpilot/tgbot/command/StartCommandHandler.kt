package com.xeno.subpilot.tgbot.command

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import com.xeno.subpilot.tgbot.ux.ScreenRenderer
import org.springframework.stereotype.Component

@Component
class StartCommandHandler(
    private val telegramClient: TelegramClient,
    private val navigationService: NavigationService,
    private val screenRenderer: ScreenRenderer,
) : BotCommand {

    override val command = "/start"
    override val description = "Start the bot"

    override fun handle(message: Message) {
        val chatId = message.chat.id
        val userName = message.from?.firstName ?: DEFAULT_USERNAME
        navigationService.clear(chatId)
        telegramClient.sendMessage(
            chatId = chatId,
            text = BotResponses.START_RESPONSE.format(userName),
        )
        screenRenderer.render(chatId, BotScreen.MAIN_MENU)
    }

    companion object {
        internal const val DEFAULT_USERNAME = "friend"
    }
}
