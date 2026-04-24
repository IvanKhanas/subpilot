package com.xeno.subpilot.tgbot.ux.callbacks

import com.xeno.subpilot.tgbot.dto.CallbackQuery
import com.xeno.subpilot.tgbot.message.CallbackHandler
import com.xeno.subpilot.tgbot.ux.BonusPurchaseService
import org.springframework.stereotype.Component

@Component
class PurchasePlanCallbackHandler(
    private val bonusPurchaseService: BonusPurchaseService,
) : CallbackHandler {

    override fun supports(data: String) = data.startsWith(CALLBACK_PREFIX)

    override suspend fun handle(callbackQuery: CallbackQuery) {
        val chatId = callbackQuery.message?.chat?.id ?: return
        val userId = callbackQuery.from?.id ?: return
        val planId = callbackQuery.data?.removePrefix(CALLBACK_PREFIX) ?: return
        bonusPurchaseService.startBonusPurchase(chatId, userId, planId)
    }

    companion object {
        const val CALLBACK_PREFIX = "purchase:"
    }
}
