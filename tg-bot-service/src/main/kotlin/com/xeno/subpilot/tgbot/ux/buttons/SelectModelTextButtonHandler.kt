package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.client.ChatClient
import com.xeno.subpilot.tgbot.client.SubscriptionClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.exception.SubscriptionServiceException
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.ux.AiProvider
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(11)
class SelectModelTextButtonHandler(
    private val telegramClient: TelegramClient,
    private val subscriptionClient: SubscriptionClient,
    private val chatClient: ChatClient,
) : TextButtonHandler {

    override fun supports(text: String) = AiProvider.findModelByDisplayName(text) != null

    override suspend fun handle(message: Message) {
        val userId = message.from?.id ?: return
        val model = AiProvider.findModelByDisplayName(message.text ?: return) ?: return
        try {
            val result = subscriptionClient.setModelPreference(userId, model.id)
            if (result.providerChanged) {
                chatClient.clearContext(message.chat.id)
            }
            val providerName = AiProvider.displayNameByKey(result.provider)
            telegramClient.sendMessage(
                message.chat.id,
                BotResponses.MODEL_SET_RESPONSE.format(
                    model.displayName,
                    model.displayName,
                    result.modelCost,
                    providerName,
                ),
                BotButtons.mainMenu,
            )
        } catch (ex: SubscriptionServiceException) {
            telegramClient.sendMessage(
                message.chat.id,
                BotResponses.MODEL_SET_FAILED_RESPONSE.text,
                BotButtons.mainMenu,
            )
        }
    }
}
