package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.client.ChatClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.ux.AiProvider
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(5)
class SelectModelTextButtonHandler(
    private val telegramClient: TelegramClient,
    private val chatClient: ChatClient,
) : TextButtonHandler {

    override fun supports(text: String) = AiProvider.findModelByDisplayName(text) != null

    override fun handle(message: Message) {
        val model = AiProvider.findModelByDisplayName(message.text ?: return) ?: return
        chatClient.setModel(message.chat.id, model.id)
        telegramClient.sendMessage(
            message.chat.id,
            BotResponses.MODEL_SET_RESPONSE.format(model.displayName),
            BotButtons.mainMenu,
        )
    }
}
