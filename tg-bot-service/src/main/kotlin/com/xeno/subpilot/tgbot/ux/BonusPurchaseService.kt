package com.xeno.subpilot.tgbot.ux

import com.xeno.subpilot.tgbot.client.LoyaltyClient
import com.xeno.subpilot.tgbot.client.SubscriptionClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.SpendResult
import com.xeno.subpilot.tgbot.exception.LoyaltyServiceException
import com.xeno.subpilot.tgbot.exception.SubscriptionServiceException
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.ux.buttons.BotButtons
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

import java.math.BigDecimal
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class BonusPurchaseService(
    private val loyaltyClient: LoyaltyClient,
    private val subscriptionClient: SubscriptionClient,
    private val planPurchaseService: PlanPurchaseService,
    private val telegramClient: TelegramClient,
) {

    suspend fun startBonusPurchase(
        chatId: Long,
        userId: Long,
        planId: String,
    ) {
        val balance = tryGetBalance(userId)
        val price = tryGetPlanPrice(planId)

        if (balance == null || price == null || balance == 0L) {
            planPurchaseService.startPayment(chatId, userId, planId)
            return
        }

        val promptText =
            if (balance >= price) {
                BotResponses.BONUS_PROMPT_RESPONSE.format(balance)
            } else {
                BotResponses.BONUS_PARTIAL_PROMPT_RESPONSE.format(balance, balance, price - balance)
            }

        val idempotencyKey = UUID.randomUUID()
        telegramClient.sendMessage(
            chatId = chatId,
            text = promptText,
            replyMarkup = BotButtons.bonusConfirmKeyboard(planId, idempotencyKey),
        )
    }

    suspend fun confirmBonusSpend(
        chatId: Long,
        userId: Long,
        planId: String,
        idempotencyKey: UUID,
        promptMessageId: Long,
        promptText: String,
    ) {
        telegramClient.editMessage(chatId, promptMessageId, promptText)
        val balance = tryGetBalance(userId) ?: 0L
        val price =
            tryGetPlanPrice(planId) ?: run {
                planPurchaseService.startPayment(chatId, userId, planId)
                return
            }

        if (balance >= price) {
            val result =
                try {
                    loyaltyClient.spend(userId, planId, idempotencyKey)
                } catch (ex: LoyaltyServiceException) {
                    logger.atError {
                        message = "loyalty_spend_call_failed"
                        cause = ex
                        payload = mapOf("user_id" to userId, "plan_id" to planId)
                    }
                    telegramClient.sendMessage(
                        chatId,
                        BotResponses.BONUS_SPEND_FAILED_RESPONSE.text,
                    )
                    return
                }
            when (result) {
                is SpendResult.Success ->
                    telegramClient.sendMessage(
                        chatId,
                        BotResponses.BONUS_SPEND_SUCCESS_RESPONSE.text,
                    )
                is SpendResult.Denied -> {
                    logger.atInfo {
                        message = "loyalty_spend_denied_fallback_to_payment"
                        payload =
                            mapOf(
                                "user_id" to userId,
                                "plan_id" to planId,
                                "reason" to result.reason.name,
                            )
                    }
                    planPurchaseService.startPayment(chatId, userId, planId)
                }
            }
        } else {
            planPurchaseService.startPayment(chatId, userId, planId, bonusPointsToApply = balance)
        }
    }

    suspend fun declineBonusSpend(
        chatId: Long,
        userId: Long,
        planId: String,
        promptMessageId: Long,
        promptText: String,
    ) {
        telegramClient.editMessage(chatId, promptMessageId, promptText)
        planPurchaseService.startPayment(chatId, userId, planId)
    }

    private suspend fun tryGetBalance(userId: Long): Long? =
        try {
            loyaltyClient.getBalance(userId)
        } catch (ex: LoyaltyServiceException) {
            logger.atWarn {
                message = "loyalty_balance_lookup_failed_fallback_to_payment"
                cause = ex
                payload = mapOf("user_id" to userId)
            }
            null
        }

    private suspend fun tryGetPlanPrice(planId: String): Long? {
        val plan =
            try {
                subscriptionClient.getPlanInfo(planId)
            } catch (ex: SubscriptionServiceException) {
                logger.atWarn {
                    message = "subscription_plan_lookup_failed_fallback_to_payment"
                    cause = ex
                    payload = mapOf("plan_id" to planId)
                }
                return null
            } ?: return null
        return BigDecimal(plan.price).toLong()
    }
}
