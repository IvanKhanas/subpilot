package com.xeno.subpilot.tgbot.ux.callbacks

import com.xeno.subpilot.tgbot.dto.CallbackQuery
import com.xeno.subpilot.tgbot.message.CallbackHandler
import com.xeno.subpilot.tgbot.ux.BonusPurchaseService
import org.springframework.stereotype.Component

import java.util.UUID

@Component
class ConfirmBonusSpendCallbackHandler(
    private val bonusPurchaseService: BonusPurchaseService,
) : CallbackHandler {

    override fun supports(data: String) = data.startsWith(CALLBACK_PREFIX)

    override suspend fun handle(callbackQuery: CallbackQuery) {
        val chatId = callbackQuery.message?.chat?.id ?: return
        val userId = callbackQuery.from?.id ?: return
        val remaining = callbackQuery.data?.removePrefix(CALLBACK_PREFIX) ?: return
        val lastColon = remaining.lastIndexOf(':')
        if (lastColon < 0) return
        val planId = remaining.substring(0, lastColon)
        val idempotencyKey = UUID.fromString(remaining.substring(lastColon + 1))
        val messageId = callbackQuery.message?.messageId ?: return
        val promptText = callbackQuery.message?.text ?: return
        bonusPurchaseService.confirmBonusSpend(
            chatId,
            userId,
            planId,
            idempotencyKey,
            messageId,
            promptText,
        )
    }

    companion object {
        const val CALLBACK_PREFIX = "bonus_yes:"
    }
}
