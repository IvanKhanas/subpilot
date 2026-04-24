package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import com.xeno.subpilot.tgbot.ux.ScreenRenderer
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(2)
class ChooseProviderTextButtonHandler(
    private val navigationService: NavigationService,
    private val screenRenderer: ScreenRenderer,
) : TextButtonHandler {

    override fun supports(text: String) = text == BotButtons.BTN_CHOOSE_MODEL

    override suspend fun handle(message: Message) {
        navigationService.push(message.chat.id, BotScreen.MAIN_MENU)
        screenRenderer.render(message.chat.id, BotScreen.PROVIDER_MENU)
    }
}
