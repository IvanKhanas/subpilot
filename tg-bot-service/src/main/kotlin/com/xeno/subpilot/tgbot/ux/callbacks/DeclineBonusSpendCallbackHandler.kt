package com.xeno.subpilot.tgbot.ux.callbacks

import com.xeno.subpilot.tgbot.dto.CallbackQuery
import com.xeno.subpilot.tgbot.message.CallbackHandler
import com.xeno.subpilot.tgbot.ux.BonusPurchaseService
import org.springframework.stereotype.Component

@Component
class DeclineBonusSpendCallbackHandler(
    private val bonusPurchaseService: BonusPurchaseService,
) : CallbackHandler {

    override fun supports(data: String) = data.startsWith(CALLBACK_PREFIX)

    override suspend fun handle(callbackQuery: CallbackQuery) {
        val chatId = callbackQuery.message?.chat?.id ?: return
        val userId = callbackQuery.from?.id ?: return
        val planId = callbackQuery.data?.removePrefix(CALLBACK_PREFIX) ?: return
        val messageId = callbackQuery.message?.messageId ?: return
        val promptText = callbackQuery.message?.text ?: return
        bonusPurchaseService.declineBonusSpend(chatId, userId, planId, messageId, promptText)
    }

    companion object {
        const val CALLBACK_PREFIX = "bonus_no:"
    }
}
