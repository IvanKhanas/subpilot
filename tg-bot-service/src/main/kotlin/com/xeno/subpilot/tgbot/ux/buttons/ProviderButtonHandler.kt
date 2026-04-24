package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.client.SubscriptionClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.PlanInfo
import com.xeno.subpilot.tgbot.exception.SubscriptionServiceException
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.ux.AiProvider
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import com.xeno.subpilot.tgbot.ux.PremiumProvider
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(10)
class ProviderButtonHandler(
    private val telegramClient: TelegramClient,
    private val subscriptionClient: SubscriptionClient,
    private val navigationService: NavigationService,
) : TextButtonHandler {

    override fun supports(text: String) =
        AiProvider.findByDisplayName(text) != null ||
            PremiumProvider.findByDisplayName(text) != null

    override suspend fun handle(message: Message) {
        val text = message.text ?: return
        val chatId = message.chat.id
        if (navigationService.peek(chatId) == BotScreen.MAIN_MENU) {
            handleModelProviderSelection(chatId, text)
        } else {
            handlePremiumProviderSelection(message, text)
        }
    }

    private fun handleModelProviderSelection(
        chatId: Long,
        text: String,
    ) {
        val provider = AiProvider.findByDisplayName(text) ?: return
        navigationService.push(chatId, BotScreen.PROVIDER_MENU)
        telegramClient.sendMessage(
            chatId,
            BotResponses.CHOOSE_MODEL_RESPONSE.format(provider.displayName),
            BotButtons.modelMenu(provider),
        )
    }

    private suspend fun handlePremiumProviderSelection(
        message: Message,
        text: String,
    ) {
        val provider = PremiumProvider.findByDisplayName(text) ?: return
        val plans =
            try {
                subscriptionClient.getPlans().filter { it.provider == provider.planProviderKey }
            } catch (ex: SubscriptionServiceException) {
                telegramClient.sendMessage(
                    message.chat.id,
                    BotResponses.AI_UNAVAILABLE_RESPONSE.text,
                )
                return
            }
        if (plans.isEmpty()) {
            telegramClient.sendMessage(message.chat.id, BotResponses.AI_UNAVAILABLE_RESPONSE.text)
            return
        }
        telegramClient.sendMessage(
            chatId = message.chat.id,
            text = BotResponses.PLAN_LIST_RESPONSE.format(provider.displayName, formatPlans(plans)),
            replyMarkup = BotButtons.planPurchaseKeyboard(plans),
        )
    }

    private fun formatPlans(plans: List<PlanInfo>): String =
        plans.joinToString("\n\n") { "${it.displayName}\nPrice: ${it.price} ${it.currency}" }
}
