package com.xeno.subpilot.tgbot.ux

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.ux.buttons.BotButtons
import org.springframework.stereotype.Component

@Component
class ScreenRenderer(
    private val telegramClient: TelegramClient,
) {
    fun render(
        chatId: Long,
        screen: BotScreen,
    ) {
        when (screen) {
            BotScreen.MAIN_MENU ->
                telegramClient.sendMessage(
                    chatId,
                    BotResponses.MAIN_MENU_RESPONSE.text,
                    BotButtons.mainMenu,
                )
            BotScreen.PROVIDER_MENU ->
                telegramClient.sendMessage(
                    chatId,
                    BotResponses.CHOOSE_PROVIDER_RESPONSE.text,
                    BotButtons.providerMenu,
                )
        }
    }
}
