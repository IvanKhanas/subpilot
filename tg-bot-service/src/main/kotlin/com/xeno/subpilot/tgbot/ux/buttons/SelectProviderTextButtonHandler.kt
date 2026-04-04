package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.ux.AiProvider
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(4)
class SelectProviderTextButtonHandler(
    private val telegramClient: TelegramClient,
    private val navigationService: NavigationService,
) : TextButtonHandler {

    override fun supports(text: String) = AiProvider.findByDisplayName(text) != null

    override fun handle(message: Message) {
        val provider = AiProvider.findByDisplayName(message.text ?: return) ?: return
        navigationService.push(message.chat.id, BotScreen.PROVIDER_MENU)
        telegramClient.sendMessage(
            message.chat.id,
            BotResponses.CHOOSE_MODEL_RESPONSE.format(provider.displayName),
            BotButtons.modelMenu(provider),
        )
    }
}
