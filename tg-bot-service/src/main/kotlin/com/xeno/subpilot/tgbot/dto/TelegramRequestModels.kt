/*
 * Copyright 2024 Ivan Khanas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xeno.subpilot.tgbot.dto

sealed interface ReplyMarkup

data class SendMessageRequest(
    val chatId: Long,
    val text: String,
    val replyMarkup: ReplyMarkup? = null,
    val parseMode: String? = null,
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

data class ReplyKeyboardRemove(
    val removeKeyboard: Boolean = true,
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

data class DeleteMessageRequest(
    val chatId: Long,
    val messageId: Long,
)

data class EditMessageTextRequest(
    val chatId: Long,
    val messageId: Long,
    val text: String,
)
