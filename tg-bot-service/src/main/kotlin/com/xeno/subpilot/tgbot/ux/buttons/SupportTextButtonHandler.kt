package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.command.SupportCommandHandler
import com.xeno.subpilot.tgbot.dto.Message
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(5)
class SupportTextButtonHandler(
    private val supportCommandHandler: SupportCommandHandler,
) : TextButtonHandler {

    override fun supports(text: String) = text == BotButtons.SUPPORT

    override fun handle(message: Message) {
        supportCommandHandler.handle(message)
    }
}
