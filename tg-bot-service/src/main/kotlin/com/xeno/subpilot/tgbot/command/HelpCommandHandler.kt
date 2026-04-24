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
package com.xeno.subpilot.tgbot.command

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.message.BotResponses
import org.springframework.stereotype.Component

@Component
class HelpCommandHandler(
    private val telegramClient: TelegramClient,
) : BotCommand {

    override val command = "/help"
    override val description = "Show available commands"

    override suspend fun handle(message: Message) {
        telegramClient.sendMessage(
            chatId = message.chat.id,
            text = BotResponses.HELP_RESPONSE.text,
        )
    }
}
