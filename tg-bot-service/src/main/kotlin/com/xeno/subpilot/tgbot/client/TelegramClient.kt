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
