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

import com.xeno.subpilot.tgbot.client.SubscriptionClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.BalanceFormatter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class BalanceCommandHandler(
    private val subscriptionClient: SubscriptionClient,
    private val telegramClient: TelegramClient,
    private val balanceFormatter: BalanceFormatter,
) : BotCommand {

    override val command = "/balance"
    override val description = "Show your request balance"

    override suspend fun handle(message: Message) {
        val userId =
            message.from?.id ?: run {
                logger.atWarn {
                    this.message = "balance_command_no_user"
                    payload = mapOf("chat_id" to message.chat.id)
                }
                return
            }
        val balance = subscriptionClient.getBalance(userId)
        telegramClient.sendMessage(message.chat.id, balanceFormatter.format(balance))
    }
}
