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

import com.xeno.subpilot.tgbot.client.LoyaltyClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.exception.LoyaltyServiceException
import com.xeno.subpilot.tgbot.message.BotResponses
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class BonusCommandHandler(
    private val loyaltyClient: LoyaltyClient,
    private val telegramClient: TelegramClient,
) : BotCommand {

    override val command = "/bonus"
    override val description = "Show your bonus points balance"

    override suspend fun handle(message: Message) {
        val userId =
            message.from?.id ?: run {
                logger.atWarn {
                    this.message = "bonus_command_no_user"
                    payload = mapOf("chat_id" to message.chat.id)
                }
                return
            }
        val balance =
            try {
                loyaltyClient.getBalance(userId)
            } catch (ex: LoyaltyServiceException) {
                logger.atError {
                    this.message = "bonus_command_get_balance_failed"
                    cause = ex
                    payload = mapOf("user_id" to userId)
                }
                telegramClient.sendMessage(
                    message.chat.id,
                    "Failed to get bonus balance. Please try again later.",
                )
                return
            }
        telegramClient.sendMessage(
            message.chat.id,
            BotResponses.BONUS_BALANCE_RESPONSE.format(balance),
        )
    }
}
