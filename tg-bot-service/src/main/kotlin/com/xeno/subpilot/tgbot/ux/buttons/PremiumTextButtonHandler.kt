package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.command.PremiumCommandHandler
import com.xeno.subpilot.tgbot.dto.Message
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(4)
class PremiumTextButtonHandler(
    private val premiumCommandHandler: PremiumCommandHandler,
) : TextButtonHandler {

    override fun supports(text: String) = text == BotButtons.PREMIUM

    override suspend fun handle(message: Message) = premiumCommandHandler.handle(message)
}
