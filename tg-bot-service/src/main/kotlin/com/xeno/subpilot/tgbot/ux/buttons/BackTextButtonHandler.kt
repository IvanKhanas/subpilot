package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import com.xeno.subpilot.tgbot.ux.ScreenRenderer
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(6)
class BackTextButtonHandler(
    private val navigationService: NavigationService,
    private val screenRenderer: ScreenRenderer,
) : TextButtonHandler {

    override fun supports(text: String) = text == BotButtons.BTN_BACK

    override fun handle(message: Message) {
        val screen = navigationService.pop(message.chat.id) ?: BotScreen.MAIN_MENU
        screenRenderer.render(message.chat.id, screen)
    }
}
