package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.command.BonusCommandHandler
import com.xeno.subpilot.tgbot.dto.Message
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(6)
class BonusTextButtonHandler(
    private val bonusCommandHandler: BonusCommandHandler,
) : TextButtonHandler {

    override fun supports(text: String) = text == BotButtons.BONUS

    override suspend fun handle(message: Message) = bonusCommandHandler.handle(message)
}
