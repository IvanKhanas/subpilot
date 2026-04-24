package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.command.MenuCommandHandler
import com.xeno.subpilot.tgbot.dto.Message
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(9)
class MenuTextButtonHandler(
    private val menuCommandHandler: MenuCommandHandler,
) : TextButtonHandler {

    override fun supports(text: String) = text == BotButtons.BTN_MAIN_MENU

    override suspend fun handle(message: Message) {
        menuCommandHandler.handle(message)
    }
}
