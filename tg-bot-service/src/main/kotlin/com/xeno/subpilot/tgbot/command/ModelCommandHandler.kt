package com.xeno.subpilot.tgbot.command

import com.xeno.subpilot.tgbot.client.ChatClient
import com.xeno.subpilot.tgbot.client.SubscriptionClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.exception.SubscriptionServiceException
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.ux.AiProvider
import org.springframework.stereotype.Component

@Component
class ModelCommandHandler(
    private val telegramClient: TelegramClient,
    private val subscriptionClient: SubscriptionClient,
    private val chatClient: ChatClient,
) : BotCommand {

    override val command = "/model"
    override val description = "Set AI model: /model <model_id>"

    override suspend fun handle(message: Message) {
        val chatId = message.chat.id
        val parts = message.text?.split(" ") ?: return
        if (parts.size != 2) {
            telegramClient.sendMessage(
                chatId,
                BotResponses.MODEL_COMMAND_USAGE_RESPONSE.format(availableModels()),
            )
            return
        }
        val modelId = parts[1]
        val model = AiProvider.findModelById(modelId)
        if (model == null) {
            telegramClient.sendMessage(
                chatId,
                BotResponses.MODEL_NOT_FOUND_RESPONSE.format(modelId, availableModels()),
            )
            return
        }
        val userId = message.from?.id ?: return
        try {
            val result = subscriptionClient.setModelPreference(userId, model.id)
            if (result.providerChanged) {
                chatClient.clearContext(chatId)
            }
            val providerName = AiProvider.displayNameByKey(result.provider)
            telegramClient.sendMessage(
                chatId,
                BotResponses.MODEL_SET_RESPONSE.format(
                    model.displayName,
                    model.displayName,
                    result.modelCost,
                    providerName,
                ),
            )
        } catch (ex: SubscriptionServiceException) {
            telegramClient.sendMessage(chatId, BotResponses.MODEL_SET_FAILED_RESPONSE.text)
        }
    }

    private fun availableModels(): String =
        AiProvider.entries.joinToString("\n\n") { provider ->
            "Provider: ${provider.displayName}\n" +
                provider.models.joinToString("\n") { "• ${it.id}" }
        }
}
