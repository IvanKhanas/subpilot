package com.xeno.subpilot.tgbot.client

import com.xeno.subpilot.tgbot.dto.BotCommandInfo
import com.xeno.subpilot.tgbot.dto.ReplyMarkup
import com.xeno.subpilot.tgbot.dto.Update

interface TelegramClient {
    fun getUpdates(
        offset: Long?,
        timeout: Int,
    ): List<Update>

    fun sendMessage(
        chatId: Long,
        text: String,
        replyMarkup: ReplyMarkup? = null,
        parseMode: String? = null,
    ): Long?

    fun editMessage(
        chatId: Long,
        messageId: Long,
        text: String,
    )

    fun deleteMessage(
        chatId: Long,
        messageId: Long,
    )

    fun answerCallbackQuery(callbackQueryId: String)

    fun setMyCommands(commands: List<BotCommandInfo>)
}
