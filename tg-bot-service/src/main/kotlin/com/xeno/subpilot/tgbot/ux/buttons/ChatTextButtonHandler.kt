package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.command.StartCommandHandler
import com.xeno.subpilot.tgbot.dto.Message
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(1)
class ChatTextButtonHandler(
    private val startCommandHandler: StartCommandHandler,
) : TextButtonHandler {

    override fun supports(text: String) = text == BotButtons.BTN_START_CHAT

    override suspend fun handle(message: Message) = startCommandHandler.registerAndGreet(message)
}
