package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.command.HelpCommandHandler
import com.xeno.subpilot.tgbot.dto.Message
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(6)
class HelpTextButtonHandler(
    private val helpCommandHandler: HelpCommandHandler,
) : TextButtonHandler {

    override fun supports(text: String) = text == BotButtons.BTN_HELP

    override suspend fun handle(message: Message) {
        helpCommandHandler.handle(message)
    }
}
