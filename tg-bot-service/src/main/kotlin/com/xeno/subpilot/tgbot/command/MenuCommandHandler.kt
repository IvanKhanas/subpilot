package com.xeno.subpilot.tgbot.command

import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import com.xeno.subpilot.tgbot.ux.ScreenRenderer
import org.springframework.stereotype.Component

@Component
class MenuCommandHandler(
    private val navigationService: NavigationService,
    private val screenRenderer: ScreenRenderer,
) : BotCommand {

    override val command = "/menu"
    override val description = "Show main menu"

    override fun handle(message: Message) {
        navigationService.clear(message.chat.id)
        screenRenderer.render(message.chat.id, BotScreen.MAIN_MENU)
    }
}
