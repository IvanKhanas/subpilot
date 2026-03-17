package com.xeno.subpilot.tgbot.dto

sealed interface ReplyMarkup

data class SendMessageRequest(
    val chatId: Long,
    val text: String,
    val replyMarkup: ReplyMarkup? = null,
)

data class InlineKeyboardMarkup(
    val inlineKeyboard: List<List<InlineKeyboardButton>>,
) : ReplyMarkup

data class InlineKeyboardButton(
    val text: String,
    val callbackData: String,
)

data class ReplyKeyboardMarkup(
    val keyboard: List<List<KeyboardButton>>,
    val resizeKeyboard: Boolean = true,
) : ReplyMarkup

data class KeyboardButton(
    val text: String,
)

data class AnswerCallbackQueryRequest(
    val callbackQueryId: String,
)

data class SetMyCommandsRequest(
    val commands: List<BotCommandInfo>,
)

data class BotCommandInfo(
    val command: String,
    val description: String,
)
