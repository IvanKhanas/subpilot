package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.command.BalanceCommandHandler
import com.xeno.subpilot.tgbot.dto.Message
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(5)
class BalanceTextButtonHandler(
    private val balanceCommandHandler: BalanceCommandHandler,
) : TextButtonHandler {

    override fun supports(text: String) = text == BotButtons.BALANCE

    override suspend fun handle(message: Message) = balanceCommandHandler.handle(message)
}
