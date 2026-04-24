package com.xeno.subpilot.tgbot.ux

import com.xeno.subpilot.tgbot.client.PaymentClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.exception.PaymentServiceException
import com.xeno.subpilot.tgbot.message.BotResponses
import org.springframework.stereotype.Component

@Component
class PlanPurchaseService(
    private val paymentClient: PaymentClient,
    private val telegramClient: TelegramClient,
) {

    suspend fun startPayment(
        chatId: Long,
        userId: Long,
        planId: String,
        bonusPointsToApply: Long = 0,
    ) {
        val confirmationUrl =
            try {
                paymentClient.createPayment(userId, planId, bonusPointsToApply)
            } catch (ex: PaymentServiceException) {
                telegramClient.sendMessage(chatId, BotResponses.PAYMENT_FAILED_RESPONSE.text)
                return
            }
        telegramClient.sendMessage(
            chatId,
            BotResponses.PAYMENT_LINK_RESPONSE.format(confirmationUrl),
        )
    }
}
