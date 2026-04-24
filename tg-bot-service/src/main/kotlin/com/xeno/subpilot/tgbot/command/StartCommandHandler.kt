package com.xeno.subpilot.tgbot.command

import com.xeno.subpilot.tgbot.client.SubscriptionClient
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
    private val subscriptionClient: SubscriptionClient,
) : BotCommand {

    override val command = "/start"
    override val description = "Start the bot"

    override suspend fun handle(message: Message) {
        navigationService.clear(message.chat.id)
        registerAndGreet(message)
        screenRenderer.render(message.chat.id, BotScreen.MAIN_MENU)
    }

    internal suspend fun registerAndGreet(message: Message) {
        val chatId = message.chat.id
        val userName = message.from?.firstName ?: DEFAULT_USERNAME
        val result = message.from?.id?.let { subscriptionClient.registerUser(it) }
        val text =
            if (result?.isNew == true) {
                BotResponses.START_NEW_USER_RESPONSE.format(
                    userName,
                    result.freeQuota,
                    result.freeProvider.displayName,
                )
            } else {
                BotResponses.START_ALREADY_REGISTERED_USER_RESPONSE.format(userName)
            }
        telegramClient.sendMessage(chatId = chatId, text = text)
    }

    companion object {
        internal const val DEFAULT_USERNAME = "friend"
    }
}
