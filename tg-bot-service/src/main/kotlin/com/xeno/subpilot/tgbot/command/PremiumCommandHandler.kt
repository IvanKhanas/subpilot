package com.xeno.subpilot.tgbot.command

import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import com.xeno.subpilot.tgbot.ux.ScreenRenderer
import org.springframework.stereotype.Component

@Component
class PremiumCommandHandler(
    private val navigationService: NavigationService,
    private val screenRenderer: ScreenRenderer,
) : BotCommand {

    override val command = "/premium"
    override val description = "Buy a subscription"

    override suspend fun handle(message: Message) {
        navigationService.clear(message.chat.id)
        screenRenderer.render(message.chat.id, BotScreen.PREMIUM_MENU)
    }
}
