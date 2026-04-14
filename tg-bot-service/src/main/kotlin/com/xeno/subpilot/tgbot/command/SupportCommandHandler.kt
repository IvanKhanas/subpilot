package com.xeno.subpilot.tgbot.command

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.properties.SupportProperties
import org.springframework.stereotype.Component

@Component
class SupportCommandHandler(
    private val telegramClient: TelegramClient,
    private val supportProperties: SupportProperties,
) : BotCommand {
    override val command = "/support"
    override val description = "Contact support"

    override fun handle(message: Message) {
        val text = BotResponses.SUPPORT_RESPONSE.text.format(supportProperties.operatorTag)

        telegramClient.sendMessage(message.chat.id, text)
    }
}
