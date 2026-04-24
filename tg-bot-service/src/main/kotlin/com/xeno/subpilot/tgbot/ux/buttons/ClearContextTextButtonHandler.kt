package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.command.ClearCommandHandler
import com.xeno.subpilot.tgbot.dto.Message
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(3)
class ClearContextTextButtonHandler(
    private val clearCommandHandler: ClearCommandHandler,
) : TextButtonHandler {

    override fun supports(text: String) = text == BotButtons.CLEAR_CONTEXT

    override suspend fun handle(message: Message) = clearCommandHandler.handle(message)
}
